package org.zstack.network.service.lb;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO;
import org.zstack.header.vm.*;
import org.zstack.identity.AccountManager;
import org.zstack.network.service.vip.*;
import org.zstack.tag.TagManager;

import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

import javax.persistence.TypedQuery;
import java.util.*;

import static java.util.Arrays.asList;

/**
 * Created by frank on 8/8/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LoadBalancerBase {
    private static final CLogger logger = Utils.getLogger(LoadBalancerBase.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private LoadBalancerManager lbMgr;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private TagManager tagMgr;

    @Autowired
    private PluginRegistry pluginRgty;

    private String getSyncId() {
        return String.format("operate-lb-%s", self.getUuid());
    }

    private LoadBalancerVO self;

    protected LoadBalancerInventory getInventory() {
        return LoadBalancerInventory.valueOf(self);
    }

    private LoadBalancerInventory reloadAndGetInventory() {
        self = dbf.reload(self);
        return getInventory();
    }

    public LoadBalancerBase(LoadBalancerVO self) {
        this.self = self;
    }

    void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof LoadBalancerActiveVmNicMsg) {
            handle((LoadBalancerActiveVmNicMsg) msg);
        } else if (msg instanceof LoadBalancerDeactiveVmNicMsg) {
            handle((LoadBalancerDeactiveVmNicMsg) msg);
        } else if (msg instanceof LoadBalancerRemoveVmNicMsg) {
            handle((LoadBalancerRemoveVmNicMsg) msg);
        } else if (msg instanceof RefreshLoadBalancerMsg) {
            handle((RefreshLoadBalancerMsg) msg);
        } else if (msg instanceof DeleteLoadBalancerMsg) {
            handle((DeleteLoadBalancerMsg) msg);
        } else if (msg instanceof DeleteLoadBalancerOnlyMsg) {
            handle((DeleteLoadBalancerOnlyMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(DeleteLoadBalancerOnlyMsg msg) {
        DeleteLoadBalancerOnlyReply reply = new DeleteLoadBalancerOnlyReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                if (self.getProviderType() == null) {
                    // not initialized yet
                    dbf.remove(self);
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                LoadBalancerBackend bkd = getBackend();
                bkd.destroyLoadBalancer(makeStruct(), new Completion(msg, chain) {
                    @Override
                    public void success() {
                        dbf.remove(self);
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "delete-load-balancer-only";
            }
        });
    }

    private void handle(final DeleteLoadBalancerMsg msg) {
        final DeleteLoadBalancerReply reply = new DeleteLoadBalancerReply();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                delete(new Completion(msg, chain) {
                    @Override
                    public void success() {
                        bus.reply(msg ,reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg ,reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "delete-lb";
            }
        });
    }

    private void handle(final RefreshLoadBalancerMsg msg) {
        final RefreshLoadBalancerReply reply = new RefreshLoadBalancerReply();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                refresh(new Completion(msg, chain) {
                    @Override
                    public void success() {
                        reply.setInventory(getInventory());
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "refresh-lb";
            }
        });
    }

    private void refresh(final Completion completion) {
        LoadBalancerBackend bkd = getBackend();
        bkd.refresh(makeStruct(), completion);
    }

    private void handle(final LoadBalancerRemoveVmNicMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final LoadBalancerRemoveVmNicReply reply = new LoadBalancerRemoveVmNicReply();
                removeNics(msg.getListenerUuid(), msg.getVmNicUuids(), new Completion(msg, chain) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "remove-nic-from-lb";
            }
        });
    }

    private void checkIfNicIsAdded(List<String> nicUuids) {
        List<String> allNicUuids = new ArrayList<String>();
        for (LoadBalancerListenerVO l : self.getListeners()) {
            allNicUuids.addAll(CollectionUtils.transformToList(l.getVmNicRefs(), new Function<String, LoadBalancerListenerVmNicRefVO>() {
                @Override
                public String call(LoadBalancerListenerVmNicRefVO arg) {
                    return arg.getVmNicUuid();
                }
            }));
        }

        for (String nicUuid : nicUuids) {
            if (!allNicUuids.contains(nicUuid)) {
                throw new CloudRuntimeException(String.format("the load balancer[uuid: %s] doesn't have a vm nic[uuid: %s] added", self.getUuid(), nicUuid));
            }
        }
    }

    private void handle(final LoadBalancerDeactiveVmNicMsg msg) {
        checkIfNicIsAdded(msg.getVmNicUuids());

        LoadBalancerListenerVO l = CollectionUtils.find(self.getListeners(), new Function<LoadBalancerListenerVO, LoadBalancerListenerVO>() {
            @Override
            public LoadBalancerListenerVO call(LoadBalancerListenerVO arg) {
                return arg.getUuid().equals(msg.getListenerUuid()) ? arg : null;
            }
        });

        final List<LoadBalancerListenerVmNicRefVO> refs = CollectionUtils.transformToList(l.getVmNicRefs(), new Function<LoadBalancerListenerVmNicRefVO, LoadBalancerListenerVmNicRefVO>() {
            @Override
            public LoadBalancerListenerVmNicRefVO call(LoadBalancerListenerVmNicRefVO arg) {
                return msg.getVmNicUuids().contains(arg.getVmNicUuid()) ? arg : null;
            }
        });

        final LoadBalancerDeactiveVmNicReply reply = new LoadBalancerDeactiveVmNicReply();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("deactive-vm-nics-on-lb-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "set-nics-to-inactive-in-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        for (LoadBalancerListenerVmNicRefVO ref : refs) {
                            ref.setStatus(LoadBalancerVmNicStatus.Inactive);
                            dbf.update(ref);
                        }

                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        for (LoadBalancerListenerVmNicRefVO ref : refs) {
                            ref.setStatus(LoadBalancerVmNicStatus.Active);
                            dbf.update(ref);
                        }

                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "deactive-nics-on-backend";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        SimpleQuery<VmNicVO> q = dbf.createQuery(VmNicVO.class);
                        q.add(VmNicVO_.uuid, Op.IN, CollectionUtils.transformToList(refs, new Function<String, LoadBalancerListenerVmNicRefVO>() {
                            @Override
                            public String call(LoadBalancerListenerVmNicRefVO arg) {
                                return arg.getVmNicUuid();
                            }
                        }));
                        List<VmNicVO> nicvos = q.list();

                        LoadBalancerBackend bkd = getBackend();
                        bkd.removeVmNics(makeStruct(), VmNicInventory.valueOf(nicvos), new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    private void activeVmNic(final LoadBalancerActiveVmNicMsg msg, final NoErrorCompletion completion) {
        checkIfNicIsAdded(msg.getVmNicUuids());

        LoadBalancerListenerVO l = CollectionUtils.find(self.getListeners(), new Function<LoadBalancerListenerVO, LoadBalancerListenerVO>() {
            @Override
            public LoadBalancerListenerVO call(LoadBalancerListenerVO arg) {
                return arg.getUuid().equals(msg.getListenerUuid()) ? arg : null;
            }
        });

        final List<LoadBalancerListenerVmNicRefVO> refs = CollectionUtils.transformToList(l.getVmNicRefs(), new Function<LoadBalancerListenerVmNicRefVO, LoadBalancerListenerVmNicRefVO>() {
            @Override
            public LoadBalancerListenerVmNicRefVO call(LoadBalancerListenerVmNicRefVO arg) {
                return msg.getVmNicUuids().contains(arg.getVmNicUuid()) ? arg : null;
            }
        });

        final LoadBalancerActiveVmNicReply reply = new LoadBalancerActiveVmNicReply();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("active-vm-nics-on-lb-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "set-nics-to-active-in-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        for (LoadBalancerListenerVmNicRefVO ref : refs) {
                            ref.setStatus(LoadBalancerVmNicStatus.Active);
                            dbf.update(ref);
                        }

                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        for (LoadBalancerListenerVmNicRefVO ref : refs) {
                            ref.setStatus(LoadBalancerVmNicStatus.Inactive);
                            dbf.update(ref);
                        }

                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "active-nics-on-backend";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        SimpleQuery<VmNicVO> q = dbf.createQuery(VmNicVO.class);
                        q.add(VmNicVO_.uuid, Op.IN, CollectionUtils.transformToList(refs, new Function<String, LoadBalancerListenerVmNicRefVO>() {
                            @Override
                            public String call(LoadBalancerListenerVmNicRefVO arg) {
                                return arg.getVmNicUuid();
                            }
                        }));
                        List<VmNicVO> nicvos = q.list();

                        LoadBalancerBackend bkd = getBackend();
                        bkd.addVmNics(makeStruct(), VmNicInventory.valueOf(nicvos), new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        bus.reply(msg, reply);
                        completion.done();
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                        completion.done();
                    }
                });
            }
        }).start();
    }

    private void handle(final LoadBalancerActiveVmNicMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                activeVmNic(msg, new NoErrorCompletion(msg, chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "deactive-nic";
            }
        });
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateLoadBalancerListenerMsg) {
            handle((APICreateLoadBalancerListenerMsg) msg);
        } else if (msg instanceof APIAddVmNicToLoadBalancerMsg) {
            handle((APIAddVmNicToLoadBalancerMsg) msg);
        } else if (msg instanceof APIRemoveVmNicFromLoadBalancerMsg) {
            handle((APIRemoveVmNicFromLoadBalancerMsg) msg);
        } else if (msg instanceof APIDeleteLoadBalancerListenerMsg) {
            handle((APIDeleteLoadBalancerListenerMsg) msg);
        } else if (msg instanceof APIDeleteLoadBalancerMsg) {
            handle((APIDeleteLoadBalancerMsg) msg);
        } else if (msg instanceof APIRefreshLoadBalancerMsg) {
            handle((APIRefreshLoadBalancerMsg) msg);
        } else if (msg instanceof APIGetCandidateVmNicsForLoadBalancerMsg) {
            handle((APIGetCandidateVmNicsForLoadBalancerMsg) msg);
        } else if (msg instanceof APIUpdateLoadBalancerMsg) {
            handle((APIUpdateLoadBalancerMsg) msg);
        } else if (msg instanceof APIUpdateLoadBalancerListenerMsg) {
            handle((APIUpdateLoadBalancerListenerMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    @Transactional(readOnly = true)
    private void handle(APIGetCandidateVmNicsForLoadBalancerMsg msg) {
        APIGetCandidateVmNicsForLoadBalancerReply reply = new APIGetCandidateVmNicsForLoadBalancerReply();

        List<String> ret = SQL.New("select vip.peerL3NetworkUuid" +
                " from VipVO vip" +
                " where vip.uuid = :uuid")
                .param("uuid", self.getVipUuid()).list();
        String peerL3Uuid = ret.isEmpty() ? null : ret.get(0);

        if (peerL3Uuid != null) {
            // the load balancer has been bound to a private L3 network
            List<VmNicVO> nics = SQL.New("select nic" +
                    " from VmNicVO nic, VmInstanceVO vm" +
                    " where nic.l3NetworkUuid = :l3Uuid" +
                    " and nic.uuid not in (select ref.vmNicUuid from LoadBalancerListenerVmNicRefVO ref where ref.listenerUuid = :luuid)" +
                    " and nic.vmInstanceUuid = vm.uuid" +
                    " and vm.type = :vmType" +
                    " and vm.state in (:vmStates)")
                    .param("l3Uuid", peerL3Uuid)
                    .param("luuid", msg.getListenerUuid())
                    .param("vmType", VmInstanceConstant.USER_VM_TYPE)
                    .param("vmStates", asList(VmInstanceState.Running, VmInstanceState.Stopped)).list();
            reply.setInventories(callGetCandidateVmNicsForLoadBalancerExtensionPoint(msg, VmNicInventory.valueOf(nics)));
            bus.reply(msg, reply);
            return;
        }

        // the load balancer has not been bound to any private L3 network
        List<String> l3Uuids = SQL.New("select l3.uuid" +
                " from L3NetworkVO l3, NetworkServiceL3NetworkRefVO ref" +
                " where l3.uuid = ref.l3NetworkUuid" +
                " and ref.networkServiceType = :type")
                .param("type", LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING).list();
        if (l3Uuids.isEmpty()) {
            reply.setInventories(new ArrayList<>());
            bus.reply(msg, reply);
            return;
        }

        new SQLBatch(){

            @Override
            protected void scripts() {
                List<String> guestNetworks = sql("select l3.uuid" +
                        " from L3NetworkVO l3, NetworkServiceL3NetworkRefVO ref" +
                        " where l3.uuid = ref.l3NetworkUuid" +
                        " and ref.networkServiceType = :type")
                        .param("type", LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING)
                        .list();

                List<VmNicVO> nics = sql("select nic" +
                        " from VmNicVO nic, VmInstanceVO vm" +
                        " where nic.l3NetworkUuid in (:guestNetworks)" +
                        " and nic.vmInstanceUuid = vm.uuid" +
                        " and vm.type = :vmType" +
                        " and vm.state in (:vmStates)")
                        .param("guestNetworks",guestNetworks)
                        .param("vmType", VmInstanceConstant.USER_VM_TYPE)
                        .param("vmStates", asList(VmInstanceState.Running, VmInstanceState.Stopped))
                        .list();

                reply.setInventories(callGetCandidateVmNicsForLoadBalancerExtensionPoint(msg, VmNicInventory.valueOf(nics)));
            }
        }.execute();

        bus.reply(msg, reply);
    }

    private List<VmNicInventory> callGetCandidateVmNicsForLoadBalancerExtensionPoint(APIGetCandidateVmNicsForLoadBalancerMsg msg, List<VmNicInventory> candidates) {
        if(candidates.isEmpty()){
            return candidates;
        }

        List<VmNicInventory> ret = candidates;
        for (GetCandidateVmNicsForLoadBalancerExtensionPoint extp : pluginRgty.getExtensionList(GetCandidateVmNicsForLoadBalancerExtensionPoint.class)) {
            ret = extp.getCandidateVmNicsForLoadBalancerInVirtualRouter(msg, ret);
        }
        return ret;
    }

    private void handle(final APIRefreshLoadBalancerMsg msg) {
        final APIRefreshLoadBalancerEvent evt = new APIRefreshLoadBalancerEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                refresh(new Completion(msg, chain) {
                    @Override
                    public void success() {
                        evt.setInventory(getInventory());
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "refresh-lb";
            }
        });
    }

    private void handle(final APIDeleteLoadBalancerMsg msg) {
        final APIDeleteLoadBalancerEvent evt = new APIDeleteLoadBalancerEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                delete(new Completion(msg, chain) {
                    @Override
                    public void success() {
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "delete-lb";
            }
        });
    }

    private void delete(final Completion completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-lb-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-lb";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        if (self.getProviderType() == null) {
                            trigger.next();
                            // not initialized yet
                            return;
                        }

                        LoadBalancerBackend bkd = getBackend();
                        bkd.destroyLoadBalancer(makeStruct(), new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "release-vip";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new Vip(self.getVipUuid()).release(new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        new SQLBatch(){

                            @Override
                            protected void scripts() {
                                for(LoadBalancerListenerVO lbListener :self.getListeners()){
                                    sql(LoadBalancerListenerVO.class)
                                            .eq(LoadBalancerListenerVO_.uuid,lbListener.getUuid())
                                            .delete();
                                }
                                sql(LoadBalancerVO.class).eq(LoadBalancerVO_.uuid,self.getUuid()).delete();
                            }
                        }.execute();
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void handle(final APIDeleteLoadBalancerListenerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                deleteListener(msg, new NoErrorCompletion(msg, chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "delete-listener";
            }
        });
    }

    private LoadBalancerStruct removeListenerStruct(LoadBalancerListenerInventory listener) {
        LoadBalancerStruct s = makeStruct();

        for (LoadBalancerListenerInventory l : s.getListeners()) {
            if (l.getUuid().equals(listener.getUuid())) {
                l.setVmNicRefs(new ArrayList<>());
            }
        }
        return s;
    }

    private void deleteListener(APIDeleteLoadBalancerListenerMsg msg, final NoErrorCompletion completion) {
        final APIDeleteLoadBalancerListenerEvent evt = new APIDeleteLoadBalancerListenerEvent(msg.getId());

        final LoadBalancerListenerVO vo = dbf.findByUuid(msg.getUuid(), LoadBalancerListenerVO.class);
        if (vo == null) {
            evt.setInventory(getInventory());
            bus.publish(evt);
            completion.done();
            return;
        }

        if (!needAction()) {
            dbf.remove(vo);
            evt.setInventory(reloadAndGetInventory());
            bus.publish(evt);
            completion.done();
            return;
        }

        LoadBalancerListenerInventory listener = LoadBalancerListenerInventory.valueOf(vo);
        LoadBalancerBackend bkd = getBackend();
        bkd.removeListener(removeListenerStruct(listener), listener, new Completion(msg, completion) {
            @Override
            public void success() {
                dbf.remove(vo);
                evt.setInventory(reloadAndGetInventory());
                bus.publish(evt);
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
                completion.done();
            }
        });
    }

    private void handle(final APIRemoveVmNicFromLoadBalancerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                removeNic(msg, new NoErrorCompletion(msg, chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "remove-nic";
            }
        });
    }

    private LoadBalancerStruct removeNicStruct(String listenerUuid, List<String> nicUuids) {
        LoadBalancerStruct s = makeStruct();
        Optional<LoadBalancerListenerInventory> opt = s.getListeners().stream().filter(it -> it.getUuid().equals(listenerUuid)).findAny();
        DebugUtils.Assert(opt.isPresent(), String.format("cannot find listener[uuid:%s]", listenerUuid));

        LoadBalancerListenerInventory l = opt.get();
        l.getVmNicRefs().removeIf(loadBalancerListenerVmNicRefInventory -> nicUuids.contains(loadBalancerListenerVmNicRefInventory.getVmNicUuid()));

        return s;
    }

    private void removeNics(String listenerUuid, final List<String> vmNicUuids, final Completion completion) {
        SimpleQuery<VmNicVO> q = dbf.createQuery(VmNicVO.class);
        q.add(VmNicVO_.uuid, Op.IN, vmNicUuids);
        List<VmNicVO> vos = q.list();
        List<VmNicInventory> nics = VmNicInventory.valueOf(vos);

        LoadBalancerBackend bkd = getBackend();
        bkd.removeVmNics(removeNicStruct(listenerUuid, vmNicUuids), nics, new Completion(completion) {
            @Override
            public void success() {
                UpdateQuery.New(LoadBalancerListenerVmNicRefVO.class)
                        .condAnd(LoadBalancerListenerVmNicRefVO_.vmNicUuid, Op.IN, vmNicUuids)
                        .condAnd(LoadBalancerListenerVmNicRefVO_.listenerUuid, Op.EQ, listenerUuid)
                        .delete();

                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void removeNic(APIRemoveVmNicFromLoadBalancerMsg msg, final NoErrorCompletion completion) {
        final APIRemoveVmNicFromLoadBalancerEvent evt = new APIRemoveVmNicFromLoadBalancerEvent(msg.getId());

        removeNics(msg.getListenerUuid(), msg.getVmNicUuids(), new Completion(msg, completion) {
            @Override
            public void success() {
                evt.setInventory(reloadAndGetInventory());
                bus.publish(evt);
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
                completion.done();
            }
        });
    }

    @Transactional(readOnly = true)
    private String findProviderTypeByVmNicUuid(String nicUuid) {
        String sql = "select l3 from L3NetworkVO l3, VmNicVO nic where nic.l3NetworkUuid = l3.uuid and nic.uuid = :uuid";
        TypedQuery<L3NetworkVO> q = dbf.getEntityManager().createQuery(sql, L3NetworkVO.class);
        q.setParameter("uuid", nicUuid);
        L3NetworkVO l3 = q.getSingleResult();
        for (NetworkServiceL3NetworkRefVO ref : l3.getNetworkServices()) {
            if (LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING.equals(ref.getNetworkServiceType())) {
                sql = "select p.type from NetworkServiceProviderVO p where p.uuid = :uuid";
                TypedQuery<String> nq = dbf.getEntityManager().createQuery(sql, String.class);
                nq.setParameter("uuid", ref.getNetworkServiceProviderUuid());
                return nq.getSingleResult();
            }
        }

        return null;
    }

    private void handle(final APIAddVmNicToLoadBalancerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                addVmNicToListener(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void addVmNicToListener(final APIAddVmNicToLoadBalancerMsg msg, final NoErrorCompletion completion) {
        final APIAddVmNicToLoadBalancerEvent evt = new APIAddVmNicToLoadBalancerEvent(msg.getId());

        final String providerType = findProviderTypeByVmNicUuid(msg.getVmNicUuids().get(0));
        if (providerType == null) {
            throw new OperationFailureException(operr("the L3 network of vm nic[uuid:%s] doesn't have load balancer service enabled", msg.getVmNicUuids().get(0)));
        }

        SimpleQuery<VmNicVO> q = dbf.createQuery(VmNicVO.class);
        q.add(VmNicVO_.uuid, Op.IN, msg.getVmNicUuids());
        List<VmNicVO> nicVOs = q.list();
        final List<VmNicInventory> nics = VmNicInventory.valueOf(nicVOs);

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("add-vm-nic-to-lb-listener-%s", msg.getListenerUuid()));
        chain.then(new ShareFlow() {
            List<LoadBalancerListenerVmNicRefVO> refs = new ArrayList<LoadBalancerListenerVmNicRefVO>();
            boolean init = false;

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "check-provider-type";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (self.getProviderType() == null) {
                            self.setProviderType(providerType);
                            self = dbf.updateAndRefresh(self);
                            init = true;
                        } else {
                            if (!providerType.equals(self.getProviderType())) {
                                throw new OperationFailureException(operr("service provider type mismatching. The load balancer[uuid:%s] is provided by the service provider[type:%s]," +
                                                " but the L3 network of vm nic[uuid:%s] is enabled with the service provider[type: %s]", self.getUuid(), self.getProviderType(),
                                        msg.getVmNicUuids().get(0), providerType));
                            }
                        }

                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (init) {
                            self = dbf.reload(self);
                            self.setProviderType(null);
                            dbf.update(self);
                        }

                        trigger.rollback();
                    }
                });

                flow(new Flow() {
                    String __name__ = "write-nic-to-db";

                    boolean s = false;

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        for (String nicUuid : msg.getVmNicUuids()) {
                            LoadBalancerListenerVmNicRefVO ref = new LoadBalancerListenerVmNicRefVO();
                            ref.setListenerUuid(msg.getListenerUuid());
                            ref.setVmNicUuid(nicUuid);
                            ref.setStatus(LoadBalancerVmNicStatus.Pending);
                            refs.add(ref);
                        }

                        dbf.persistCollection(refs);
                        s = true;
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (s) {
                            dbf.removeCollection(refs, LoadBalancerListenerVmNicRefVO.class);
                        }
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "add-nic-to-lb";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        LoadBalancerBackend bkd = getBackend();
                        LoadBalancerStruct s = makeStruct();
                        s.setInit(init);
                        bkd.addVmNics(s, nics, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg, completion) {
                    @Override
                    public void handle(Map data) {
                        for (LoadBalancerListenerVmNicRefVO ref : refs) {
                            ref.setStatus(LoadBalancerVmNicStatus.Active);
                        }

                        dbf.updateCollection(refs);
                        evt.setInventory(LoadBalancerListenerInventory.valueOf(dbf.findByUuid(msg.getListenerUuid(), LoadBalancerListenerVO.class)));
                        bus.publish(evt);
                        completion.done();
                    }
                });

                error(new FlowErrorHandler(msg, completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setError(errCode);
                        bus.publish(evt);
                        completion.done();
                    }
                });
            }
        }).start();
    }

    private boolean needAction() {
        if (self.getProviderType() == null) {
            return false;
        }

        LoadBalancerListenerVmNicRefVO activeNic = CollectionUtils.find(self.getListeners(), new Function<LoadBalancerListenerVmNicRefVO, LoadBalancerListenerVO>() {
            @Override
            public LoadBalancerListenerVmNicRefVO call(LoadBalancerListenerVO arg) {
                for (LoadBalancerListenerVmNicRefVO ref : arg.getVmNicRefs()) {
                    if (ref.getStatus() == LoadBalancerVmNicStatus.Active || ref.getStatus() == LoadBalancerVmNicStatus.Pending) {
                        return ref;
                    }
                }
                return null;
            }
        });

        if (activeNic == null) {
            return false;
        }

        return true;
    }

    private LoadBalancerBackend getBackend() {
        DebugUtils.Assert(self.getProviderType() != null, "providerType cannot be null");
        return lbMgr.getBackend(self.getProviderType());
    }

    private LoadBalancerStruct makeStruct() {
        LoadBalancerStruct struct = new LoadBalancerStruct();
        struct.setLb(reloadAndGetInventory());

        List<String> activeNicUuids = new ArrayList<String>();
        for (LoadBalancerListenerVO l : self.getListeners()) {
            activeNicUuids.addAll(CollectionUtils.transformToList(l.getVmNicRefs(), new Function<String, LoadBalancerListenerVmNicRefVO>() {
                @Override
                public String call(LoadBalancerListenerVmNicRefVO arg) {
                    return arg.getStatus() == LoadBalancerVmNicStatus.Active || arg.getStatus() == LoadBalancerVmNicStatus.Pending ? arg.getVmNicUuid() : null;
                }
            }));
        }

        if (activeNicUuids.isEmpty()) {
            struct.setVmNics(new HashMap<String, VmNicInventory>());
        } else {
            SimpleQuery<VmNicVO> nq = dbf.createQuery(VmNicVO.class);
            nq.add(VmNicVO_.uuid, Op.IN, activeNicUuids);
            List<VmNicVO> nicvos = nq.list();
            Map<String, VmNicInventory> m = new HashMap<String, VmNicInventory>();
            for (VmNicVO n : nicvos) {
                m.put(n.getUuid(), VmNicInventory.valueOf(n));
            }
            struct.setVmNics(m);
        }

        struct.setListeners(LoadBalancerListenerInventory.valueOf(self.getListeners()));

        return struct;
    }

    private void handle(final APICreateLoadBalancerListenerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                createListener(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "create-listener";
            }
        });
    }

    private void createListener(final APICreateLoadBalancerListenerMsg msg, final NoErrorCompletion completion) {
        final APICreateLoadBalancerListenerEvent evt = new APICreateLoadBalancerListenerEvent(msg.getId());
        LoadBalancerListenerVO vo = new LoadBalancerListenerVO();
        vo.setLoadBalancerUuid(self.getUuid());
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setDescription(vo.getDescription());
        vo.setName(msg.getName());
        vo.setInstancePort(msg.getInstancePort());
        vo.setLoadBalancerPort(msg.getLoadBalancerPort());
        vo.setProtocol(msg.getProtocol());
        vo = dbf.persistAndRefresh(vo);
        acntMgr.createAccountResourceRef(msg.getSession().getAccountUuid(), vo.getUuid(), LoadBalancerListenerVO.class);
        tagMgr.createNonInherentSystemTags(msg.getSystemTags(), vo.getUuid(), LoadBalancerListenerVO.class.getSimpleName());
        evt.setInventory(LoadBalancerListenerInventory.valueOf(vo));
        bus.publish(evt);
        completion.done();
    }

    private void handle(APIUpdateLoadBalancerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIUpdateLoadBalancerEvent evt = new APIUpdateLoadBalancerEvent(msg.getId());

                final LoadBalancerInventory lb = new LoadBalancerInventory();

                boolean update = false;
                if (msg.getName() != null) {
                    self.setName(msg.getName());
                    update = true;
                }
                if (msg.getDescription() != null) {
                    self.setDescription(msg.getDescription());
                    update = true;
                }

                if (update) {
                    dbf.update(self);
                }

                evt.setInventory(getInventory());
                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getName() {
                return "update-lb-info";
            }
        });
    }

    private void handle(APIUpdateLoadBalancerListenerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIUpdateLoadBalancerListenerEvent evt = new APIUpdateLoadBalancerListenerEvent(msg.getId());
                LoadBalancerListenerVO lblVo = dbf.findByUuid(msg.getUuid(), LoadBalancerListenerVO.class);
                boolean update = false;
                if (msg.getName() != null) {
                    lblVo.setName(msg.getName());
                    update = true;
                }
                if (msg.getDescription() != null) {
                    lblVo.setDescription(msg.getDescription());
                    update = true;
                }
                if (update) {
                    dbf.update(lblVo);
                }

                evt.setInventory( LoadBalancerListenerInventory.valueOf(lblVo));
                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getName() {
                return "update-lb-listener";
            }
        });
    }
}
