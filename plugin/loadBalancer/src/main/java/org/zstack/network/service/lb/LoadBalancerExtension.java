package org.zstack.network.service.lb;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.AbstractNetworkServiceExtension;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Created by frank on 8/13/2015.
 */
public class LoadBalancerExtension extends AbstractNetworkServiceExtension {
    private static final CLogger logger = Utils.getLogger(LoadBalancerExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    @Override
    public NetworkServiceType getNetworkServiceType() {
        return LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE;
    }

    @Override
    public void applyNetworkService(VmInstanceSpec servedVm, Map<String, Object> data, final Completion completion) {
        SimpleQuery<LoadBalancerVmNicRefVO> q = dbf.createQuery(LoadBalancerVmNicRefVO.class);
        q.add(LoadBalancerVmNicRefVO_.vmNicUuid, Op.IN, CollectionUtils.transformToList(servedVm.getDestNics(), new Function<String, VmNicInventory>() {
            @Override
            public String call(VmNicInventory arg) {
                return arg.getUuid();
            }
        }));
        q.groupBy(LoadBalancerVmNicRefVO_.loadBalancerUuid);
        List<LoadBalancerVmNicRefVO> lbs = q.list();
        if (lbs.isEmpty()) {
            completion.success();
            return;
        }

        Map<String, List<String>> m = new HashMap<String, List<String>>();
        for (LoadBalancerVmNicRefVO l : lbs) {
            List<String> nics = m.get(l.getLoadBalancerUuid());
            if (nics == null) {
                nics = new ArrayList<String>();
                m.put(l.getLoadBalancerUuid(), nics);
            }
            nics.add(l.getVmNicUuid());
        }


        List<LoadBalancerActiveVmNicMsg> msgs = CollectionUtils.transformToList(m.entrySet(), new Function<LoadBalancerActiveVmNicMsg, Entry<String, List<String>>>() {
            @Override
            public LoadBalancerActiveVmNicMsg call(Entry<String, List<String>> arg) {
                LoadBalancerActiveVmNicMsg msg = new LoadBalancerActiveVmNicMsg();
                msg.setLoadBalancerUuid(arg.getKey());
                msg.setVmNicUuids(arg.getValue());
                bus.makeTargetServiceIdByResourceUuid(msg, LoadBalancerConstants.SERVICE_ID, arg.getKey());
                return msg;
            }
        });

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("active-nic-for-vm-%s-on-lb", servedVm.getVmInventory().getUuid()));
        for (final LoadBalancerActiveVmNicMsg msg : msgs) {
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
                public void rollback(final FlowTrigger trigger, Map data) {
                    if (!s) {
                        LoadBalancerDeactiveVmNicMsg dmsg = new LoadBalancerDeactiveVmNicMsg();
                        dmsg.setLoadBalancerUuid(msg.getLoadBalancerUuid());
                        dmsg.setVmNicUuids(msg.getVmNicUuids());
                        bus.makeTargetServiceIdByResourceUuid(dmsg, LoadBalancerConstants.SERVICE_ID, msg.getLoadBalancerUuid());
                        bus.send(dmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    //TODO: clean up
                                    logger.warn(String.format("failed to deactive vm nics[uuids: %s] on the load balancer[uuid:%s]", msg.getVmNicUuids(), msg.getLoadBalancerUuid()));
                                }
                            }
                        });
                    }

                    trigger.rollback();
                }
            });

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
    }

    @Override
    public void releaseNetworkService(VmInstanceSpec servedVm, Map<String, Object> data, final NoErrorCompletion completion) {
        SimpleQuery<LoadBalancerVmNicRefVO> q = dbf.createQuery(LoadBalancerVmNicRefVO.class);
        q.add(LoadBalancerVmNicRefVO_.vmNicUuid, Op.IN, CollectionUtils.transformToList(servedVm.getDestNics(), new Function<String, VmNicInventory>() {
            @Override
            public String call(VmNicInventory arg) {
                return arg.getUuid();
            }
        }));
        q.groupBy(LoadBalancerVmNicRefVO_.loadBalancerUuid);
        List<LoadBalancerVmNicRefVO> lbs = q.list();
        if (lbs.isEmpty()) {
            completion.done();
            return;
        }

        final Map<String, List<String>> m = new HashMap<String, List<String>>();
        for (LoadBalancerVmNicRefVO l : lbs) {
            List<String> nics = m.get(l.getLoadBalancerUuid());
            if (nics == null) {
                nics = new ArrayList<String>();
                m.put(l.getLoadBalancerUuid(), nics);
            }
            nics.add(l.getVmNicUuid());
        }

        List<NeedReplyMessage> msgs = new ArrayList<NeedReplyMessage>();
        if (servedVm.getCurrentVmOperation() == VmOperation.Destroy) {
            msgs.addAll(CollectionUtils.transformToList(m.entrySet(), new Function<NeedReplyMessage, Entry<String, List<String>>>() {
                @Override
                public NeedReplyMessage call(Entry<String, List<String>> arg) {
                    LoadBalancerRemoveVmNicMsg msg = new LoadBalancerRemoveVmNicMsg();
                    msg.setVmNicUuids(arg.getValue());
                    msg.setLoadBalancerUuid(arg.getKey());
                    bus.makeTargetServiceIdByResourceUuid(msg, LoadBalancerConstants.SERVICE_ID, arg.getKey());
                    return msg;
                }
            }));
        } else {
            msgs.addAll(CollectionUtils.transformToList(m.entrySet(), new Function<LoadBalancerDeactiveVmNicMsg, Entry<String, List<String>>>() {
                @Override
                public LoadBalancerDeactiveVmNicMsg call(Entry<String, List<String>> arg) {
                    LoadBalancerDeactiveVmNicMsg msg = new LoadBalancerDeactiveVmNicMsg();
                    msg.setVmNicUuids(arg.getValue());
                    msg.setLoadBalancerUuid(arg.getKey());
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
}
