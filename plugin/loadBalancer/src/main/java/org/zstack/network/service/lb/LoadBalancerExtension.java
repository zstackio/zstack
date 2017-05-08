package org.zstack.network.service.lb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.AbstractNetworkServiceExtension;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipReleaseExtensionPoint;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Created by frank on 8/13/2015.
 */
public class LoadBalancerExtension extends AbstractNetworkServiceExtension implements VipReleaseExtensionPoint {
    private static final CLogger logger = Utils.getLogger(LoadBalancerExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    @Override
    public NetworkServiceType getNetworkServiceType() {
        return LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE;
    }

    @Transactional(readOnly = true)
    private List<Tuple> getLbTuple(VmInstanceSpec servedVm) {
        String sql = "select l.uuid, l.loadBalancerUuid, ref.vmNicUuid from LoadBalancerListenerVmNicRefVO ref, LoadBalancerListenerVO l where ref.listenerUuid = l.uuid" +
                " and ref.vmNicUuid in (:nicUuids)";
        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("nicUuids", CollectionUtils.transformToList(servedVm.getDestNics(), new Function<String, VmNicInventory>() {
            @Override
            public String call(VmNicInventory arg) {
                return arg.getUuid();
            }
        }));
        return q.getResultList();
    }

    @Override
    public void applyNetworkService(final VmInstanceSpec servedVm, Map<String, Object> data, final Completion completion) {
        List<Tuple> ts = getLbTuple(servedVm);
        if (ts.isEmpty()) {
            completion.success();
            return;
        }

        Map<String, LoadBalancerActiveVmNicMsg> m = new HashMap<String, LoadBalancerActiveVmNicMsg>();
        for (Tuple t : ts) {
            String listenerUuid = t.get(0, String.class);
            String lbUuid =  t.get(1, String.class);
            String nicUuid = t.get(2, String.class);
            LoadBalancerActiveVmNicMsg msg = m.get(listenerUuid);
            if (msg == null) {
                msg = new LoadBalancerActiveVmNicMsg();
                msg.setLoadBalancerUuid(lbUuid);
                msg.setListenerUuid(listenerUuid);
                msg.setVmNicUuids(new ArrayList<String>());
                bus.makeTargetServiceIdByResourceUuid(msg, LoadBalancerConstants.SERVICE_ID, lbUuid);
                m.put(listenerUuid, msg);
            }
            msg.getVmNicUuids().add(nicUuid);
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("active-nic-for-vm-%s-on-lb", servedVm.getVmInventory().getUuid()));
        for (final LoadBalancerActiveVmNicMsg msg : m.values()) {
            chain.then(new Flow() {
                boolean s = false;

                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    bus.send(msg, new CloudBusCallBack(trigger) {
                        @Override
                        public void run(MessageReply reply) {
                            if (reply.isSuccess()) {
                                s = true;
                                trigger.next();
                            } else {
                                trigger.fail(reply.getError());
                            }
                        }
                    });
                }

                @Override
                public void rollback(final FlowRollback trigger, Map data) {
                    if (!s) {
                        LoadBalancerDeactiveVmNicMsg dmsg = new LoadBalancerDeactiveVmNicMsg();
                        dmsg.setLoadBalancerUuid(msg.getLoadBalancerUuid());
                        dmsg.setListenerUuid(msg.getListenerUuid());
                        dmsg.setVmNicUuids(msg.getVmNicUuids());
                        bus.makeTargetServiceIdByResourceUuid(dmsg, LoadBalancerConstants.SERVICE_ID, msg.getLoadBalancerUuid());
                        bus.send(dmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    //TODO: add GC
                                    logger.warn(String.format("failed to deactive vm nics[uuids: %s] on the load balancer[uuid:%s]", msg.getVmNicUuids(), msg.getLoadBalancerUuid()));
                                }
                            }
                        });
                    }

                    trigger.rollback();
                }
            });
        }

        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    public void releaseNetworkService(VmInstanceSpec servedVm, Map<String, Object> data, final NoErrorCompletion completion) {
        List<Tuple> ts = getLbTuple(servedVm);
        if (ts.isEmpty()) {
            completion.done();
            return;
        }

        class Triplet {
            String lbUuid;
            String listenerUuid;
            List<String> vmNicUuids;
        }

        Map<String, Triplet> mt = new HashMap<String, Triplet>();
        for (Tuple t : ts) {
            String listenerUuid = t.get(0, String.class);
            Triplet tr = mt.get(listenerUuid);
            if (tr == null) {
                tr = new Triplet();
                tr.listenerUuid = listenerUuid;
                tr.lbUuid = t.get(1, String.class);
                tr.vmNicUuids = new ArrayList<String>();
                mt.put(listenerUuid, tr);
            }
            tr.vmNicUuids.add(t.get(2, String.class));
        }

        List<NeedReplyMessage> msgs = new ArrayList<NeedReplyMessage>();
        if (servedVm.getCurrentVmOperation() == VmOperation.Destroy) {
            msgs.addAll(CollectionUtils.transformToList(mt.entrySet(), new Function<NeedReplyMessage, Entry<String, Triplet>>() {
                @Override
                public NeedReplyMessage call(Entry<String, Triplet> arg) {
                    LoadBalancerRemoveVmNicMsg msg = new LoadBalancerRemoveVmNicMsg();
                    msg.setVmNicUuids(arg.getValue().vmNicUuids);
                    msg.setListenerUuid(arg.getValue().listenerUuid);
                    msg.setLoadBalancerUuid(arg.getValue().lbUuid);
                    bus.makeTargetServiceIdByResourceUuid(msg, LoadBalancerConstants.SERVICE_ID, arg.getKey());
                    return msg;
                }
            }));
        } else {
            msgs.addAll(CollectionUtils.transformToList(mt.entrySet(), new Function<LoadBalancerDeactiveVmNicMsg, Entry<String, Triplet>>() {
                @Override
                public LoadBalancerDeactiveVmNicMsg call(Entry<String, Triplet> arg) {
                    LoadBalancerDeactiveVmNicMsg msg = new LoadBalancerDeactiveVmNicMsg();
                    msg.setVmNicUuids(arg.getValue().vmNicUuids);
                    msg.setListenerUuid(arg.getValue().listenerUuid);
                    msg.setLoadBalancerUuid(arg.getValue().lbUuid);
                    bus.makeTargetServiceIdByResourceUuid(msg, LoadBalancerConstants.SERVICE_ID, arg.getKey());
                    return msg;
                }
            }));
        }

        bus.send(msgs, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                for (MessageReply r : replies) {
                    if (!r.isSuccess()) {
                        //TODO
                        logger.warn(String.format("failed to deactive a vm nic on lb, need a cleanup, %s", r.getError()));
                    }
                }

                completion.done();
            }
        });
    }

    @Override
    public String getVipUse() {
        return LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING;
    }

    @Override
    public void releaseServicesOnVip(VipInventory vip, final Completion completion) {
        SimpleQuery<LoadBalancerVO> q = dbf.createQuery(LoadBalancerVO.class);
        q.select(LoadBalancerVO_.uuid);
        q.add(LoadBalancerVO_.vipUuid, Op.EQ, vip.getUuid());
        String lbUuid = q.findValue();
        if (lbUuid == null) {
            completion.success();
            return;
        }

        DeleteLoadBalancerOnlyMsg msg = new DeleteLoadBalancerOnlyMsg();
        msg.setLoadBalancerUuid(lbUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, LoadBalancerConstants.SERVICE_ID, lbUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }
}
