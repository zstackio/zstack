package org.zstack.network.service.lb;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
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
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.identity.AccountManager;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.function.Function;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 8/8/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LoadBalancerBase {
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

    private LoadBalancerVO self;

    private String getSyncId() {
        return String.format("operate-lb-%s", self.getUuid());
    }

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
        } else {
            bus.dealWithUnknownMessage(msg);
        }
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
                removeNics(msg.getVmNicUuids(), new Completion(msg, chain) {
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
        for (String nicUuid : nicUuids) {
            boolean s = false;
            for (LoadBalancerVmNicRefVO ref : self.getVmNicRefs()) {
                if (nicUuid.equals(ref.getVmNicUuid())) {
                    s = true;
                    break;
                }
            }

            if (!s) {
                throw new CloudRuntimeException(String.format("the load balancer[uuid: %s] doesn't have a vm nic[uuid: %s] added", self.getUuid(), nicUuid));
            }
        }
    }

    private void handle(final LoadBalancerDeactiveVmNicMsg msg) {
        checkIfNicIsAdded(msg.getVmNicUuids());

        final List<LoadBalancerVmNicRefVO> refs = CollectionUtils.transformToList(self.getVmNicRefs(), new Function<LoadBalancerVmNicRefVO, LoadBalancerVmNicRefVO>() {
            @Override
            public LoadBalancerVmNicRefVO call(LoadBalancerVmNicRefVO arg) {
                return msg.getVmNicUuids().contains(arg.getVmNicUuid()) ?  arg : null;
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
                        for (LoadBalancerVmNicRefVO ref : refs) {
                            ref.setStatus(LoadBalancerVmNicStatus.Inactive);
                            dbf.update(ref);
                        }

                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowTrigger trigger, Map data) {
                        for (LoadBalancerVmNicRefVO ref : refs) {
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
                        q.add(VmNicVO_.uuid, Op.IN, CollectionUtils.transformToList(refs, new Function<String, LoadBalancerVmNicRefVO>() {
                            @Override
                            public String call(LoadBalancerVmNicRefVO arg) {
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

    private void deactiveVmNic(final LoadBalancerActiveVmNicMsg msg, final NoErrorCompletion completion) {
        checkIfNicIsAdded(msg.getVmNicUuids());

        final List<LoadBalancerVmNicRefVO> refs = CollectionUtils.transformToList(self.getVmNicRefs(), new Function<LoadBalancerVmNicRefVO, LoadBalancerVmNicRefVO>() {
            @Override
            public LoadBalancerVmNicRefVO call(LoadBalancerVmNicRefVO arg) {
                return msg.getVmNicUuids().contains(arg.getVmNicUuid()) ?  arg : null;
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
                        for (LoadBalancerVmNicRefVO ref : refs) {
                            ref.setStatus(LoadBalancerVmNicStatus.Active);
                            dbf.update(ref);
                        }

                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowTrigger trigger, Map data) {
                        for (LoadBalancerVmNicRefVO ref : refs) {
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
                        q.add(VmNicVO_.uuid, Op.IN, CollectionUtils.transformToList(refs, new Function<String, LoadBalancerVmNicRefVO>() {
                            @Override
                            public String call(LoadBalancerVmNicRefVO arg) {
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
                deactiveVmNic(msg, new NoErrorCompletion(msg, chain) {
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
        } else if (msg instanceof APIRemoveNicFromLoadBalancerMsg) {
            handle((APIRemoveNicFromLoadBalancerMsg) msg);
        } else if (msg instanceof APIDeleteLoadBalancerListenerMsg) {
            handle((APIDeleteLoadBalancerListenerMsg) msg);
        } else if (msg instanceof APIDeleteLoadBalancerMsg) {
            handle((APIDeleteLoadBalancerMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final APIDeleteLoadBalancerMsg msg) {
        thdf.chainSubmit(new ChainTask() {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                delete(msg, new NoErrorCompletion(msg, chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
                chain.next();
            }

            @Override
            public String getName() {
                return "delete-lb";
            }
        });
    }

    private void delete(APIDeleteLoadBalancerMsg msg, final NoErrorCompletion completion) {
        final APIDeleteLoadBalancerEvent evt = new APIDeleteLoadBalancerEvent(msg.getId());
        LoadBalancerBackend bkd = getBackend();
        bkd.destroyLoadBalancer(makeStruct(), new Completion(msg, completion) {
            @Override
            public void success() {
                dbf.remove(self);
                dbf.removeCollection(self.getListeners(), LoadBalancerListenerVO.class);
                bus.publish(evt);
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setErrorCode(errorCode);
                completion.done();
            }
        });
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
        Iterator<LoadBalancerListenerInventory> it = s.getListeners().iterator();
        while (it.hasNext()) {
            if (it.next().getUuid().equals(listener.getUuid())) {
                it.remove();
            }
        }
        return s;
    }

    private void deleteListener(APIDeleteLoadBalancerListenerMsg msg, final NoErrorCompletion completion) {
        final APIDeleteLoadBalancerListenerEvent evt = new APIDeleteLoadBalancerListenerEvent(msg.getId());

        final LoadBalancerListenerVO vo = dbf.findByUuid(msg.getListenerUuid(), LoadBalancerListenerVO.class);
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
                evt.setErrorCode(errorCode);
                bus.publish(evt);
                completion.done();
            }
        });
    }

    private void handle(final APIRemoveNicFromLoadBalancerMsg msg) {
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

    private LoadBalancerStruct removeNicStruct(List<String> nicUuids) {
        LoadBalancerStruct s = makeStruct();
        Iterator<VmNicInventory> it = s.getVmNics().iterator();
        while (it.hasNext()) {
            if (nicUuids.contains(it.next().getUuid())) {
                it.remove();
            }
        }
        return s;
    }

    private void removeNics(final List<String> vmNicUuids, final Completion completion) {
        SimpleQuery<VmNicVO> q = dbf.createQuery(VmNicVO.class);
        q.add(VmNicVO_.uuid, Op.IN, vmNicUuids);
        List<VmNicVO> vos = q.list();
        List<VmNicInventory> nics = VmNicInventory.valueOf(vos);

        LoadBalancerBackend bkd = getBackend();
        bkd.removeVmNics(removeNicStruct(vmNicUuids), nics, new Completion(completion) {
            @Override
            public void success() {
                SimpleQuery<LoadBalancerVmNicRefVO> q = dbf.createQuery(LoadBalancerVmNicRefVO.class);
                q.add(LoadBalancerVmNicRefVO_.vmNicUuid, Op.IN, vmNicUuids);
                List<LoadBalancerVmNicRefVO> refs = q.list();
                dbf.removeCollection(refs, LoadBalancerVmNicRefVO.class);
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void removeNic(APIRemoveNicFromLoadBalancerMsg msg, final NoErrorCompletion completion) {
        final APIRemoveNicFromLoadBalancerEvent evt = new APIRemoveNicFromLoadBalancerEvent(msg.getId());

        SimpleQuery<LoadBalancerVmNicRefVO> q = dbf.createQuery(LoadBalancerVmNicRefVO.class);
        q.add(LoadBalancerVmNicRefVO_.vmNicUuid, Op.EQ, msg.getVmNicUuid());
        if (!q.isExists()) {
            evt.setInventory(reloadAndGetInventory());
            bus.publish(evt);
            return;
        }

        removeNics(list(msg.getVmNicUuid()), new Completion(msg, completion) {
            @Override
            public void success() {
                evt.setInventory(reloadAndGetInventory());
                bus.publish(evt);
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setErrorCode(errorCode);
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
                addVmNicToLoadBalancer(msg, new NoErrorCompletion(chain) {
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

    private void addVmNicToLoadBalancer(final APIAddVmNicToLoadBalancerMsg msg, final NoErrorCompletion completion) {
        final APIAddVmNicToLoadBalancerEvent evt = new APIAddVmNicToLoadBalancerEvent(msg.getId());

        final String providerType = findProviderTypeByVmNicUuid(msg.getVmNicUuid());
        if (providerType == null) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("the L3 network of vm nic[uuid:%s] doesn't have load balancer service enabled", msg.getVmNicUuid())
            ));
        }

        final VmNicInventory nic = VmNicInventory.valueOf(dbf.findByUuid(msg.getVmNicUuid(), VmNicVO.class));
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("add-vm-nic-%s-to-lb-%s", msg.getVmNicUuid(), self.getUuid()));
        chain.then(new ShareFlow() {
            LoadBalancerVmNicRefVO ref;
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
                                throw new OperationFailureException(errf.stringToOperationError(
                                        String.format("service provider type mismatching. The load balancer[uuid:%s] is provided by the service provider[type:%s]," +
                                                        " but the L3 network of vm nic[uuid:%s] is enabled with the service provider[type: %s]", self.getUuid(), self.getProviderType(),
                                                msg.getVmNicUuid(), providerType)
                                ));
                            }
                        }

                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowTrigger trigger, Map data) {
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
                        ref = new LoadBalancerVmNicRefVO();
                        ref.setLoadBalancerUuid(self.getUuid());
                        ref.setVmNicUuid(nic.getUuid());
                        ref.setStatus(LoadBalancerVmNicStatus.Pending);
                        ref = dbf.persistAndRefresh(ref);
                        s = true;
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowTrigger trigger, Map data) {
                        if (s) {
                            dbf.remove(ref);
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
                        bkd.addVmNic(s, nic, new Completion(trigger) {
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
                        ref.setStatus(LoadBalancerVmNicStatus.Active);
                        dbf.update(ref);

                        evt.setInventory(reloadAndGetInventory());
                        bus.publish(evt);
                        completion.done();
                    }
                });

                error(new FlowErrorHandler(msg, completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setErrorCode(errCode);
                        bus.publish(evt);
                        completion.done();
                    }
                });
            }
        }).start();
    }

    private boolean needAction() {
        return self.getProviderType() != null && !self.getVmNicRefs().isEmpty();
    }

    private LoadBalancerBackend getBackend() {
        DebugUtils.Assert(self.getProviderType() != null, "providerType cannot be null");
        return lbMgr.getBackend(self.getProviderType());
    }

    private LoadBalancerStruct makeStruct() {
        LoadBalancerStruct struct = new LoadBalancerStruct();
        struct.setLb(reloadAndGetInventory());

        if (!self.getVmNicRefs().isEmpty()) {
            List<String> activeNics = CollectionUtils.transformToList(self.getVmNicRefs(), new Function<String, LoadBalancerVmNicRefVO>() {
                @Override
                public String call(LoadBalancerVmNicRefVO arg) {
                    return arg.getStatus() == LoadBalancerVmNicStatus.Active ? arg.getVmNicUuid() : null;
                }
            });

            if (!activeNics.isEmpty()) {
                SimpleQuery<VmNicVO> nq = dbf.createQuery(VmNicVO.class);
                nq.add(VmNicVO_.uuid, Op.IN, activeNics);
                List<VmNicVO> nics = nq.list();
                struct.setVmNics(VmNicInventory.valueOf(nics));
            } else {
                struct.setVmNics(new ArrayList<VmNicInventory>());
            }
        } else {
            struct.setVmNics(new ArrayList<VmNicInventory>());
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

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-listener-lb-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            LoadBalancerListenerVO vo;

            @Override
            public void setup() {
                flow(new Flow() {
                    boolean s = false;

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        vo = new LoadBalancerListenerVO();
                        vo.setLoadBalancerUuid(self.getUuid());
                        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
                        vo.setDescription(vo.getDescription());
                        vo.setName(msg.getName());
                        vo.setInstancePort(msg.getInstancePort());
                        vo.setLoadBalancerPort(msg.getLoadBalancerPort());
                        vo.setProtocol(msg.getProtocol());
                        dbf.persist(vo);

                        s = true;
                        acntMgr.createAccountResourceRef(msg.getSession().getAccountUuid(), vo.getUuid(), LoadBalancerListenerVO.class);
                        tagMgr.createNonInherentSystemTags(msg.getSystemTags(), vo.getUuid(), LoadBalancerListenerVO.class.getSimpleName());
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowTrigger trigger, Map data) {
                        if (s) {
                            dbf.remove(vo);
                        }
                        trigger.rollback();
                    }
                });

                if (needAction()) {
                    flow(new NoRollbackFlow() {
                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            LoadBalancerBackend bkd = getBackend();
                            bkd.addListener(makeStruct(), LoadBalancerListenerInventory.valueOf(vo), new Completion(trigger) {
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
                }

                done(new FlowDoneHandler(msg, completion) {
                    @Override
                    public void handle(Map data) {
                        evt.setInventory(reloadAndGetInventory());
                        bus.publish(evt);
                        completion.done();
                    }
                });

                error(new FlowErrorHandler(msg, completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setErrorCode(errCode);
                        bus.publish(evt);
                        completion.done();
                    }
                });
            }
        }).start();
    }
}
