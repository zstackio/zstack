package org.zstack.network.service.lb;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
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
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.*;
import org.zstack.identity.AccountManager;
import org.zstack.network.l3.L3NetworkManager;
import org.zstack.network.service.vip.ModifyVipAttributesStruct;
import org.zstack.network.service.vip.Vip;
import org.zstack.network.service.vip.VipVO;
import org.zstack.tag.SystemTagCreator;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.*;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

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
    protected CascadeFacade casf;
    @Autowired
    protected L3NetworkManager l3Mgr;

    @Autowired
    private PluginRegistry pluginRgty;

    public static String getSyncId(String vipUuid) {
        return String.format("operate-lb-with-vip-%s", vipUuid);
    }

    private String getSyncId() {
        return getSyncId(self.getVipUuid());
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
        } else if (msg instanceof LoadBalancerChangeCertificateMsg) {
            handle((LoadBalancerChangeCertificateMsg) msg);
        } else if (msg instanceof AddVmNicToLoadBalancerMsg) {
            handle((AddVmNicToLoadBalancerMsg) msg);
        } else if (msg instanceof LoadBalancerGetPeerL3NetworksMsg) {
            handle((LoadBalancerGetPeerL3NetworksMsg) msg);
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
                if (dbf.reload(self) == null) {
                    /*the lb has been deleted by  the previous task*/
                    chain.next();
                    return;
                }

                if (self.getProviderType() == null) {
                    // not initialized yet
                    deleteListenersForLoadBalancer(msg.getLoadBalancerUuid());
                    dbf.remove(Q.New(LoadBalancerVO.class).eq(LoadBalancerVO_.uuid, msg.getLoadBalancerUuid()).find());
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                LoadBalancerBackend bkd = getBackend();
                bkd.destroyLoadBalancer(makeStruct(), new Completion(msg, chain) {
                    @Override
                    public void success() {
                        deleteListenersForLoadBalancer(msg.getLoadBalancerUuid());
                        dbf.removeByPrimaryKey(msg.getLoadBalancerUuid(), LoadBalancerVO.class);
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

    private void deleteListenersForLoadBalancer(String lbUuid) {
        List<LoadBalancerListenerVO> listenerVOS = Q.New(LoadBalancerListenerVO.class)
                .eq(LoadBalancerListenerVO_.loadBalancerUuid, lbUuid)
                .list();
        if (listenerVOS != null && !listenerVOS.isEmpty()) {
            logger.debug(String.format("delete loadBalancerListeners[%s] for loadBalancer[uuid:%s]",
                    listenerVOS.stream().map(vo -> vo.getUuid()).collect(Collectors.toList()), lbUuid));
            listenerVOS.forEach(vo -> {
                /*there is no cascade deleting configure for acl ref in db */
                if (!vo.getAclRefs().isEmpty()) {
                    dbf.removeCollection(vo.getAclRefs(), LoadBalancerListenerACLRefVO.class);
                }
                dbf.removeByPrimaryKey(vo.getUuid(), LoadBalancerListenerVO.class);
            });
        }
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
                if (dbf.reload(self) == null) {
                    /*the lb has been deleted by  the previous task*/
                    chain.next();
                    return;
                }

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
                removeNics(msg.getListenerUuids(), msg.getVmNicUuids(), new Completion(msg, chain) {
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

        List<LoadBalancerListenerVO> lbls = CollectionUtils.transformToList(self.getListeners(), new Function<LoadBalancerListenerVO, LoadBalancerListenerVO>() {
            @Override
            public LoadBalancerListenerVO call(LoadBalancerListenerVO arg) {
                return msg.getListenerUuids().contains(arg.getUuid()) ? arg : null;
            }
        });

        final List<LoadBalancerListenerVmNicRefVO> refs = new ArrayList<>();
        for (LoadBalancerListenerVO lbl : lbls) {
            refs.addAll(lbl.getVmNicRefs().stream().filter(ref -> msg.getVmNicUuids().contains(ref.getVmNicUuid())).collect(Collectors.toList()));
        }

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
        } else if (msg instanceof APIGetCandidateL3NetworksForLoadBalancerMsg) {
            handle((APIGetCandidateL3NetworksForLoadBalancerMsg) msg);
        } else if (msg instanceof APIUpdateLoadBalancerMsg) {
            handle((APIUpdateLoadBalancerMsg) msg);
        } else if (msg instanceof APIUpdateLoadBalancerListenerMsg) {
            handle((APIUpdateLoadBalancerListenerMsg) msg);
        } else if (msg instanceof APIAddCertificateToLoadBalancerListenerMsg) {
            handle((APIAddCertificateToLoadBalancerListenerMsg) msg);
        } else if (msg instanceof APIRemoveCertificateFromLoadBalancerListenerMsg) {
            handle((APIRemoveCertificateFromLoadBalancerListenerMsg) msg);
        } else if (msg instanceof APIRemoveAccessControlListFromLoadBalancerMsg) {
            handle((APIRemoveAccessControlListFromLoadBalancerMsg) msg);
        } else if (msg instanceof APIAddAccessControlListToLoadBalancerMsg) {
            handle((APIAddAccessControlListToLoadBalancerMsg) msg);
        } else if (msg instanceof APIChangeLoadBalancerListenerMsg) {
            handle((APIChangeLoadBalancerListenerMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetCandidateL3NetworksForLoadBalancerMsg msg) {
        APIGetCandidateL3NetworksForLoadBalancerReply reply = new APIGetCandidateL3NetworksForLoadBalancerReply();
        LoadBalancerGetPeerL3NetworksMsg amsg = new LoadBalancerGetPeerL3NetworksMsg();
        amsg.setLoadBalancerUuid(msg.getLoadBalancerUuid());
        bus.makeTargetServiceIdByResourceUuid(amsg, LoadBalancerConstants.SERVICE_ID, msg.getLoadBalancerUuid());
        LoadBalancerGetPeerL3NetworksReply areply = (LoadBalancerGetPeerL3NetworksReply)bus.call(amsg);
        if (areply.isSuccess()) {
            reply.setInventories(msg.filter(areply.getInventories()));
        } else {
            reply.setError(areply.getError());
        }
        bus.reply(msg, reply);
    }

    @Transactional(readOnly = true)
    private void handle(final LoadBalancerGetPeerL3NetworksMsg msg) {
        LoadBalancerGetPeerL3NetworksReply reply = new LoadBalancerGetPeerL3NetworksReply();
        new SQLBatch(){
            @Override
            protected void scripts() {
                List<L3NetworkVO> guestNetworks = sql("select l3" +
                        " from L3NetworkVO l3, NetworkServiceL3NetworkRefVO ref" +
                        " where l3.uuid = ref.l3NetworkUuid" +
                        " and ref.networkServiceType = :type")
                        .param("type", LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING)
                        .list();

                List<L3NetworkInventory> ret = L3NetworkInventory.valueOf(guestNetworks);
                if (ret != null && !ret.isEmpty()) {
                    for (GetPeerL3NetworksForLoadBalancerExtensionPoint extp : pluginRgty.getExtensionList(GetPeerL3NetworksForLoadBalancerExtensionPoint.class)) {
                        ret = extp.getPeerL3NetworksForLoadBalancer(self.getUuid(), ret);
                    }
                }
                reply.setInventories(ret);
            }
        }.execute();

        bus.reply(msg, reply);
    }

    @Transactional(readOnly = true)
    private void handle(APIGetCandidateVmNicsForLoadBalancerMsg msg) {
        APIGetCandidateVmNicsForLoadBalancerReply reply = new APIGetCandidateVmNicsForLoadBalancerReply();

        new SQLBatch(){
            @Override
            protected void scripts() {
                List<String> guestNetworks = sql("select l3.uuid" +
                        " from L3NetworkVO l3, NetworkServiceL3NetworkRefVO ref" +
                        " where l3.uuid = ref.l3NetworkUuid" +
                        " and ref.networkServiceType = :type")
                        .param("type", LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING)
                        .list();

                List<VmNicVO> nics = new ArrayList<>();
                if (guestNetworks != null && !guestNetworks.isEmpty()) {
                    nics = sql("select nic" +
                            " from VmNicVO nic, VmInstanceVO vm" +
                            " where nic.l3NetworkUuid in (:guestNetworks)" +
                            " and nic.vmInstanceUuid = vm.uuid" +
                            " and vm.type = :vmType" +
                            " and vm.state in (:vmStates)")
                            .param("guestNetworks",guestNetworks)
                            .param("vmType", VmInstanceConstant.USER_VM_TYPE)
                            .param("vmStates", asList(VmInstanceState.Running, VmInstanceState.Stopped))
                            .list();
                }

                /* TODO: lb only support ipv4 */
                LoadBalancerVO lbVO = dbf.findByUuid(msg.getLoadBalancerUuid(), LoadBalancerVO.class);
                VipVO vipVO = dbf.findByUuid(lbVO.getVipUuid(), VipVO.class);
                List<VmNicInventory> nicInvs;
                if (IPv6NetworkUtils.isIpv6Address(vipVO.getIp())) {
                    nicInvs = new ArrayList<>();
                } else {
                    nicInvs = l3Mgr.filterVmNicByIpVersion(VmNicInventory.valueOf(nics), IPv6Constants.IPv4);
                }

                reply.setInventories(callGetCandidateVmNicsForLoadBalancerExtensionPoint(msg, nicInvs));
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

        final String issuer = LoadBalancerVO.class.getSimpleName();
        final List<LoadBalancerInventory> ctx = Arrays.asList(LoadBalancerInventory.valueOf(self));
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-loadBalancer-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                if (msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Permissive) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "delete-loadBalancer-permissive-check";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            casf.asyncCascade(CascadeConstant.DELETION_CHECK_CODE, issuer, ctx, new Completion(trigger) {
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
                        String __name__ = "delete-loadBalancer-permissive-delete";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            casf.asyncCascade(CascadeConstant.DELETION_DELETE_CODE, issuer, ctx, new Completion(trigger) {
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
                } else {
                    flow(new NoRollbackFlow() {
                        String __name__ = "delete-loadBalancer-force-delete";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            casf.asyncCascade(CascadeConstant.DELETION_FORCE_DELETE_CODE, issuer, ctx, new Completion(trigger) {
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

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                        bus.publish(evt);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setError(err(SysErrors.DELETE_RESOURCE_ERROR, errCode, errCode.getDetails()));
                        bus.publish(evt);
                    }
                });
            }
        }).start();
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
                    @Transactional(readOnly = true)
                    private List<String> getGuestNetworkUuids() {
                        if (self.getListeners().isEmpty()) {
                            return new ArrayList<>();
                        }

                        return new SQLBatchWithReturn<List<String>>() {
                            @Override
                            protected List<String> scripts() {
                                List<String> guestNetworkUuids = sql("select distinct ip.l3NetworkUuid" +
                                        " from UsedIpVO ip, VmNicVO nic, LoadBalancerListenerVO listener," +
                                        " LoadBalancerListenerVmNicRefVO nicRef where" +
                                        " ip.vmNicUuid = nic.uuid and listener.uuid = nicRef.listenerUuid and nicRef.vmNicUuid = nic.uuid" +
                                        " and listener.loadBalancerUuid = :lb")
                                        .param("lb", self.getUuid()).list();
                                return guestNetworkUuids;
                            }
                        }.execute();
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
                        struct.setUseFor(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING);
                        struct.setServiceUuid(self.getUuid());
                        if (self.getProviderType() != null) {
                            /*release vip peer networks*/
                            struct.setServiceProvider(self.getProviderType());
                            List<String> guestNetworkUuids = getGuestNetworkUuids();
                            if (!guestNetworkUuids.isEmpty()) {
                                struct.setPeerL3NetworkUuids(guestNetworkUuids);
                            }
                        }
                        Vip v = new Vip(self.getVipUuid());
                        v.setStruct(struct);
                        v.release(new Completion(trigger) {
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
                                    /*there is no cascade deleting configure for acl ref in db */
                                    for(LoadBalancerListenerACLRefVO acl: lbListener.getAclRefs()) {
                                        sql(LoadBalancerListenerACLRefVO.class)
                                                .eq(LoadBalancerListenerACLRefVO_.id, acl.getId())
                                                .delete();
                                    }
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
            /*there is no cascade deleting configure for acl ref in db */
            if (!vo.getAclRefs().isEmpty()) {
                dbf.removeCollection(vo.getAclRefs(), LoadBalancerListenerACLRefVO.class);
            }
            /*http://jira.zstack.io/browse/ZSTAC-27065*/
            dbf.removeByPrimaryKey(vo.getUuid(), LoadBalancerListenerVO.class);
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
                /*there is no cascade deleting configure for acl ref in db */
                if (!vo.getAclRefs().isEmpty()) {
                    dbf.removeCollection(vo.getAclRefs(), LoadBalancerListenerACLRefVO.class);
                }
                dbf.removeByPrimaryKey(vo.getUuid(), LoadBalancerListenerVO.class);
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

    private LoadBalancerStruct removeNicStruct(List<String> listenerUuids, List<String> nicUuids) {
        LoadBalancerStruct s = makeStruct();
        for (LoadBalancerListenerInventory l : s.getListeners()) {
            if (!listenerUuids.contains(l.getUuid())) {
                continue;
            }

            l.getVmNicRefs().removeIf(loadBalancerListenerVmNicRefInventory -> nicUuids.contains(loadBalancerListenerVmNicRefInventory.getVmNicUuid()));
        }

        return s;
    }

    private void removeNics(List<String> listenerUuids, final List<String> vmNicUuids, final Completion completion) {
        SimpleQuery<VmNicVO> q = dbf.createQuery(VmNicVO.class);
        q.add(VmNicVO_.uuid, Op.IN, vmNicUuids);
        List<VmNicVO> vos = q.list();
        List<VmNicInventory> nics = VmNicInventory.valueOf(vos);

        LoadBalancerBackend bkd = getBackend();
        bkd.removeVmNics(removeNicStruct(listenerUuids, vmNicUuids), nics, new Completion(completion) {
            @Override
            public void success() {
                UpdateQuery.New(LoadBalancerListenerVmNicRefVO.class)
                        .condAnd(LoadBalancerListenerVmNicRefVO_.vmNicUuid, Op.IN, vmNicUuids)
                        .condAnd(LoadBalancerListenerVmNicRefVO_.listenerUuid, Op.IN, listenerUuids)
                        .delete();
                listenerUuids.stream().forEach(listenerUuid -> new LoadBalancerWeightOperator().deleteNicsWeight(vmNicUuids, listenerUuid));
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

        removeNics(Arrays.asList(msg.getListenerUuid()), msg.getVmNicUuids(), new Completion(msg, completion) {
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
        final APIAddVmNicToLoadBalancerEvent evt = new APIAddVmNicToLoadBalancerEvent(msg.getId());

        AddVmNicToLoadBalancerMsg addVmNicToLoadBalancerMsg = new AddVmNicToLoadBalancerMsg();
        addVmNicToLoadBalancerMsg.setLoadBalancerUuid(msg.getLoadBalancerUuid());
        addVmNicToLoadBalancerMsg.setListenerUuid(msg.getListenerUuid());
        addVmNicToLoadBalancerMsg.setVmNicUuids(msg.getVmNicUuids());
        addVmNicToLoadBalancerMsg.setSystemTags(msg.getSystemTags());
        bus.makeLocalServiceId(addVmNicToLoadBalancerMsg, LoadBalancerConstants.SERVICE_ID);

        bus.send(addVmNicToLoadBalancerMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    evt.setInventory(LoadBalancerListenerInventory.valueOf(dbf.findByUuid(msg.getListenerUuid(), LoadBalancerListenerVO.class)));
                    bus.publish(evt);
                    return;
                }

                evt.setError(reply.getError());
                bus.publish(evt);
            }
        });
    }

    private void handle(final AddVmNicToLoadBalancerMsg msg) {
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

    private void addVmNicToListener(final AddVmNicToLoadBalancerMsg msg, final NoErrorCompletion completion) {
        final AddVmNicToLoadBalancerReply reply = new AddVmNicToLoadBalancerReply();

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
            List<LoadBalancerListenerVmNicRefVO> refs = new ArrayList<>();
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
                        if (msg.getSystemTags() != null) {
                            new LoadBalancerWeightOperator().setWeight(msg.getSystemTags(), msg.getListenerUuid());
                        }
                        s = true;
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (s) {
                            dbf.removeCollection(refs, LoadBalancerListenerVmNicRefVO.class);
                            if (msg.getSystemTags() != null) {
                                new LoadBalancerWeightOperator().deleteWeight(msg.getSystemTags(), msg.getListenerUuid());
                            }
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
                        reply.setSuccess(true);
                        bus.reply(msg, reply);
                        completion.done();
                    }
                });

                error(new FlowErrorHandler(msg, completion) {
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
            Map<String, VmNicInventory> m = new HashMap<>();
            for (VmNicVO n : nicvos) {
                m.put(n.getUuid(), VmNicInventory.valueOf(n));
            }
            struct.setVmNics(m);
        }

        Map<String, List<String>> systemTags = new HashMap<>();
        for (LoadBalancerListenerVO l : self.getListeners()) {
            SimpleQuery<SystemTagVO> q  = dbf.createQuery(SystemTagVO.class);
            q.select(SystemTagVO_.tag);
            q.add(SystemTagVO_.resourceUuid, Op.EQ, l.getUuid());
            q.add(SystemTagVO_.resourceType, Op.EQ, LoadBalancerListenerVO.class.getSimpleName());
            systemTags.put(l.getUuid(), q.listValue());
        }
        struct.setTags(systemTags);

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
                if (Q.New(LoadBalancerListenerVO.class)
                        .eq(LoadBalancerListenerVO_.loadBalancerUuid, msg.getLoadBalancerUuid())
                        .eq(LoadBalancerListenerVO_.protocol, msg.getProtocol())
                        .eq(LoadBalancerListenerVO_.loadBalancerPort, msg.getLoadBalancerPort()).isExists()) {
                    APICreateLoadBalancerListenerEvent evt = new APICreateLoadBalancerListenerEvent(msg.getId());
                    evt.setError(argerr("there is listener with same port [%s] and same load balancer [uuid:%s]",
                            msg.getLoadBalancerPort(), msg.getLoadBalancerUuid()));
                    bus.publish(evt);

                    chain.next();
                    return;
                }

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
        vo.setDescription(msg.getDescription());
        vo.setName(msg.getName());
        vo.setInstancePort(msg.getInstancePort());
        vo.setLoadBalancerPort(msg.getLoadBalancerPort());
        vo.setProtocol(msg.getProtocol());
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        vo = dbf.persistAndRefresh(vo);
        if (msg.getCertificateUuid() != null) {
            LoadBalancerListenerCertificateRefVO ref = new LoadBalancerListenerCertificateRefVO();
            ref.setListenerUuid(vo.getUuid());
            ref.setCertificateUuid(msg.getCertificateUuid());
            dbf.persist(ref);
        }

        if (msg.getAclUuids() != null) {
            final String listenerUuid = vo.getUuid();
            List<LoadBalancerListenerACLRefVO> refs = new ArrayList<>();
            msg.getAclUuids().stream().forEach(aclUuid -> {
                LoadBalancerListenerACLRefVO ref = new LoadBalancerListenerACLRefVO();
                ref.setAclUuid(aclUuid);
                ref.setType(LoadBalancerAclType.valueOf(msg.getAclType()));
                ref.setListenerUuid(listenerUuid);
                refs.add(ref);
            });
            dbf.persistCollection(refs);
        }

        tagMgr.createNonInherentSystemTags(msg.getSystemTags(), vo.getUuid(), LoadBalancerListenerVO.class.getSimpleName());
        vo = dbf.updateAndRefresh(vo);
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

                evt.setInventory(LoadBalancerListenerInventory.valueOf(lblVo));
                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getName() {
                return "update-lb-listener";
            }
        });
    }

    private void handle(APIAddAccessControlListToLoadBalancerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                DebugUtils.Assert(msg.getAclType() != null && msg.getAclUuids() != null, "parameters cannot be null");
                APIAddAccessControlListToLoadBalancerEvent evt = new APIAddAccessControlListToLoadBalancerEvent(msg.getId());
                List<LoadBalancerListenerACLRefVO> refs = new ArrayList<>();
                msg.getAclUuids().stream().forEach(aclUuid -> {
                    LoadBalancerListenerACLRefVO ref = new LoadBalancerListenerACLRefVO();
                    ref.setAclUuid(aclUuid);
                    ref.setType(LoadBalancerAclType.valueOf(msg.getAclType()));
                    ref.setListenerUuid(msg.getListenerUuid());
                    refs.add(ref);
                });
                dbf.persistCollection(refs);

                final LoadBalancerListenerVO lblVo = dbf.findByUuid(msg.getListenerUuid(), LoadBalancerListenerVO.class);

                Boolean refresh = Q.New(LoadBalancerListenerVmNicRefVO.class).eq(LoadBalancerListenerVmNicRefVO_.listenerUuid, msg.getListenerUuid()).isExists();
                if (refresh) {
                    RefreshLoadBalancerMsg rmsg = new RefreshLoadBalancerMsg();
                    rmsg.setUuid(msg.getLoadBalancerUuid());
                    bus.makeLocalServiceId(rmsg, LoadBalancerConstants.SERVICE_ID);
                    bus.send(rmsg, new CloudBusCallBack(chain) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format("update listener [uuid:%s] failed", msg.getLoadBalancerUuid()));
                                evt.setError(reply.getError());
                                dbf.removeCollection(refs, LoadBalancerListenerACLRefVO.class);
                            } else {
                                evt.setInventory(LoadBalancerListenerInventory.valueOf(lblVo));
                            }
                            bus.publish(evt);
                        }
                    });
                    chain.next();
                    return;
                }

                evt.setInventory( LoadBalancerListenerInventory.valueOf(lblVo));
                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getName() {
                return "add-acl-lb-listener";
            }
        });
    }

    private void handle(APIRemoveAccessControlListFromLoadBalancerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIRemoveAccessControlListFromLoadBalancerEvent evt = new APIRemoveAccessControlListFromLoadBalancerEvent(msg.getId());
                List<LoadBalancerListenerACLRefVO> refs = Q.New(LoadBalancerListenerACLRefVO.class).in(LoadBalancerListenerACLRefVO_.aclUuid, msg.getAclUuids())
                                                           .eq(LoadBalancerListenerACLRefVO_.listenerUuid, msg.getListenerUuid()).list();

                if (refs.isEmpty()) {
                    final LoadBalancerListenerVO lblVo = dbf.findByUuid(msg.getListenerUuid(), LoadBalancerListenerVO.class);
                    evt.setInventory(LoadBalancerListenerInventory.valueOf(lblVo));
                    bus.publish(evt);
                    chain.next();
                    return;
                }
                dbf.removeCollection(refs, LoadBalancerListenerACLRefVO.class);

                final LoadBalancerListenerVO lblVo = dbf.findByUuid(msg.getListenerUuid(), LoadBalancerListenerVO.class);

                Boolean refresh = Q.New(LoadBalancerListenerVmNicRefVO.class).eq(LoadBalancerListenerVmNicRefVO_.listenerUuid, msg.getListenerUuid()).isExists();
                if (refresh) {
                    RefreshLoadBalancerMsg rmsg = new RefreshLoadBalancerMsg();
                    rmsg.setUuid(msg.getLoadBalancerUuid());
                    bus.makeLocalServiceId(rmsg, LoadBalancerConstants.SERVICE_ID);
                    bus.send(rmsg, new CloudBusCallBack(chain) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format("update listener [uuid:%s] failed", msg.getLoadBalancerUuid()));
                                evt.setError(reply.getError());
                                dbf.persistCollection(refs);
                            } else {
                                evt.setInventory(LoadBalancerListenerInventory.valueOf(lblVo));
                            }
                            bus.publish(evt);
                        }
                    });

                    chain.next();
                    return;
                }

                evt.setInventory(LoadBalancerListenerInventory.valueOf(lblVo));
                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getName() {
                return "remove-acl-lb-listener";
            }
        });
    }

    private String[] getHeathCheckTarget(String ListenerUuid) {
        String target = LoadBalancerSystemTags.HEALTH_TARGET.getTokenByResourceUuid(ListenerUuid,
                LoadBalancerSystemTags.HEALTH_TARGET_TOKEN);
        DebugUtils.Assert(target != null, String.format("the health target not exist, please check if the listener[%s] exist", ListenerUuid));

        String[] ts = target.split(":");
        if (ts.length != 2) {
            throw new OperationFailureException(argerr("invalid health target[%s], the format is targetCheckProtocol:port, for example, tcp:default", target));
        }
        return ts;
    }

    private void handle(APIChangeLoadBalancerListenerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIChangeLoadBalancerListenerEvent evt = new APIChangeLoadBalancerListenerEvent(msg.getId());
                LoadBalancerListenerVO lblVo = dbf.findByUuid(msg.getUuid(), LoadBalancerListenerVO.class);

                if (msg.getBalancerAlgorithm() != null) {
                    LoadBalancerSystemTags.BALANCER_ALGORITHM.update(msg.getUuid(),
                            LoadBalancerSystemTags.BALANCER_ALGORITHM.instantiateTag(map(
                                    e(LoadBalancerSystemTags.BALANCER_ALGORITHM_TOKEN, msg.getBalancerAlgorithm())
                            )));
                }

                if (msg.getConnectionIdleTimeout() != null) {
                    LoadBalancerSystemTags.CONNECTION_IDLE_TIMEOUT.update(msg.getUuid(),
                            LoadBalancerSystemTags.CONNECTION_IDLE_TIMEOUT.instantiateTag(map(
                                    e(LoadBalancerSystemTags.CONNECTION_IDLE_TIMEOUT_TOKEN, msg.getConnectionIdleTimeout())
                            )));
                }

                if (msg.getHealthCheckInterval() != null) {
                    LoadBalancerSystemTags.HEALTH_INTERVAL.update(msg.getUuid(),
                            LoadBalancerSystemTags.HEALTH_INTERVAL.instantiateTag(map(
                                    e(LoadBalancerSystemTags.HEALTH_INTERVAL_TOKEN, msg.getHealthCheckInterval())
                            )));
                }

                if (msg.getHealthCheckTarget() != null) {
                    String[] ts = getHeathCheckTarget(msg.getLoadBalancerListenerUuid());
                    LoadBalancerSystemTags.HEALTH_TARGET.update(msg.getUuid(),
                            LoadBalancerSystemTags.HEALTH_TARGET.instantiateTag(map(
                                    e(LoadBalancerSystemTags.HEALTH_TARGET_TOKEN, String.format("%s:%s", ts[0], msg.getHealthCheckTarget())))
                            ));
                }

                if (msg.getHealthyThreshold() != null) {
                    LoadBalancerSystemTags.HEALTHY_THRESHOLD.update(msg.getUuid(),
                            LoadBalancerSystemTags.HEALTHY_THRESHOLD.instantiateTag(map(
                                    e(LoadBalancerSystemTags.HEALTHY_THRESHOLD_TOKEN, msg.getHealthyThreshold())
                            )));
                }

                if (msg.getUnhealthyThreshold() != null) {
                    LoadBalancerSystemTags.UNHEALTHY_THRESHOLD.update(msg.getUuid(),
                            LoadBalancerSystemTags.UNHEALTHY_THRESHOLD.instantiateTag(map(
                                    e(LoadBalancerSystemTags.UNHEALTHY_THRESHOLD_TOKEN, msg.getUnhealthyThreshold())
                            )));
                }

                if (msg.getMaxConnection() != null) {
                    LoadBalancerSystemTags.MAX_CONNECTION.update(msg.getUuid(),
                            LoadBalancerSystemTags.MAX_CONNECTION.instantiateTag(map(
                                    e(LoadBalancerSystemTags.MAX_CONNECTION_TOKEN, msg.getMaxConnection())
                            )));
                }

                String[] ts = getHeathCheckTarget(msg.getUuid());
                if (msg.getHealthCheckProtocol() != null && !msg.getHealthCheckProtocol().equals(ts[0])) {
                    if (LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_TCP.equals(ts[0]) &&
                            LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_HTTP.equals(msg.getHealthCheckProtocol())) {
                        DebugUtils.Assert(msg.getHealthCheckMethod() != null && msg.getHealthCheckURI() != null,
                            String.format("the http health check protocol must be specified its healthy checking parameters including healthCheckMethod and healthCheckURI"));
                        String code = LoadBalancerConstants.HealthCheckStatusCode.http_2xx.toString();
                        if (msg.getHealthCheckHttpCode() != null) {
                            code = msg.getHealthCheckHttpCode();
                        }
                        SystemTagCreator creator = LoadBalancerSystemTags.HEALTH_PARAMETER.newSystemTagCreator(msg.getUuid());
                        creator.setTagByTokens(map(e(LoadBalancerSystemTags.HEALTH_PARAMETER_TOKEN,
                                String.format("%s:%s:%s", msg.getHealthCheckMethod(), msg.getHealthCheckURI(), code))
                            ));
                        creator.create();
                    }

                    if (LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_HTTP.equals(ts[0]) &&
                            LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_TCP.equals(msg.getHealthCheckProtocol())) {
                        LoadBalancerSystemTags.HEALTH_PARAMETER.delete(msg.getUuid());
                    }

                    LoadBalancerSystemTags.HEALTH_TARGET.update(msg.getUuid(),
                                LoadBalancerSystemTags.HEALTH_TARGET.instantiateTag(map(
                                        e(LoadBalancerSystemTags.HEALTH_TARGET_TOKEN, String.format("%s:%s", msg.getHealthCheckProtocol(), ts[1])))
                                ));
                    ts = getHeathCheckTarget(msg.getUuid());
                }

                if (msg.getHealthCheckHttpCode() != null || msg.getHealthCheckMethod() != null || msg.getHealthCheckURI() != null) {
                    if (LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_HTTP.equals(ts[0])) {
                        String param = LoadBalancerSystemTags.HEALTH_PARAMETER.getTokenByResourceUuid(msg.getLoadBalancerListenerUuid(),
                                LoadBalancerSystemTags.HEALTH_PARAMETER_TOKEN);
                        String[] pm = param.split(":");
                        if (pm.length != 3) {
                            throw new OperationFailureException(argerr("invalid health checking parameters[%s], the format is method:URI:code, for example, GET:/index.html:http_2xx", param));
                        }

                        if (msg.getHealthCheckMethod() != null) {
                            pm[0] = msg.getHealthCheckMethod();
                        }
                        if (msg.getHealthCheckURI() != null) {
                            pm[1] = msg.getHealthCheckURI();
                        }
                        if (msg.getHealthCheckHttpCode() != null) {
                             pm[2] = msg.getHealthCheckHttpCode();
                        }
                        LoadBalancerSystemTags.HEALTH_PARAMETER.update(msg.getUuid(),
                                LoadBalancerSystemTags.HEALTH_PARAMETER.instantiateTag(
                                        map(e(LoadBalancerSystemTags.HEALTH_PARAMETER_TOKEN, String.format("%s:%s:%s", pm[0], pm[1], pm[2]))
                                        )));
                    }
                }

                if (msg.getAclStatus() != null) {
                    if (LoadBalancerSystemTags.BALANCER_ACL.hasTag(msg.getUuid())) {
                        LoadBalancerSystemTags.BALANCER_ACL.update(msg.getUuid(),
                                LoadBalancerSystemTags.BALANCER_ACL.instantiateTag(map(
                                        e(LoadBalancerSystemTags.BALANCER_ACL_TOKEN, msg.getAclStatus())
                                )));
                    } else {
                        SystemTagCreator creator = LoadBalancerSystemTags.BALANCER_ACL.newSystemTagCreator(msg.getUuid());
                        creator.setTagByTokens(map(
                                e(LoadBalancerSystemTags.BALANCER_ACL_TOKEN, msg.getAclStatus())
                        ));
                        creator.inherent = false;
                        creator.create();
                    }
                }

                if (msg.getSystemTags() != null) {
                    new LoadBalancerWeightOperator().setWeight(msg.getSystemTags(), msg.getLoadBalancerListenerUuid());
                }

                Boolean refresh = Q.New(LoadBalancerListenerVmNicRefVO.class).eq(LoadBalancerListenerVmNicRefVO_.listenerUuid, msg.getUuid()).isExists();
                if (refresh) {
                    RefreshLoadBalancerMsg msg = new RefreshLoadBalancerMsg();
                    msg.setUuid(lblVo.getLoadBalancerUuid());
                    bus.makeLocalServiceId(msg, LoadBalancerConstants.SERVICE_ID);
                    bus.send(msg, new CloudBusCallBack(chain) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format( "update listener [uuid:%s] failed", lblVo.getUuid()));
                                evt.setError(reply.getError());
                            } else {
                                evt.setInventory(LoadBalancerListenerInventory.valueOf(lblVo));
                            }
                            bus.publish(evt);
                        }
                    });

                    chain.next();
                    return;
                }
                evt.setInventory( LoadBalancerListenerInventory.valueOf(lblVo));
                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getName() {
                return "change-lb-listener";
            }
        });
    }

    private void handle(APIAddCertificateToLoadBalancerListenerMsg msg) {
        APIAddCertificateToLoadBalancerListenerEvent evt = new APIAddCertificateToLoadBalancerListenerEvent(msg.getId());

        LoadBalancerListenerCertificateRefVO ref = Q.New(LoadBalancerListenerCertificateRefVO.class)
                .eq(LoadBalancerListenerCertificateRefVO_.listenerUuid, msg.getListenerUuid())
                .eq(LoadBalancerListenerCertificateRefVO_.certificateUuid, msg.getCertificateUuid()).limit(1).find();
        if (ref != null) {
            LoadBalancerListenerInventory inv = LoadBalancerListenerInventory.valueOf(dbf.findByUuid(msg.getListenerUuid(), LoadBalancerListenerVO.class));
            evt.setInventory(inv);
            bus.publish(evt);
            return;
        } else {
            ref = new LoadBalancerListenerCertificateRefVO();
            ref.setListenerUuid(msg.getListenerUuid());
            ref.setCertificateUuid(msg.getCertificateUuid());
            dbf.persist(ref);
        }

        final LoadBalancerListenerCertificateRefVO original_ref = ref;
        LoadBalancerChangeCertificateMsg cmsg = new LoadBalancerChangeCertificateMsg();
        cmsg.setListenerUuid(msg.getListenerUuid());
        cmsg.setLoadBalancerUuid(msg.getLoadBalancerUuid());
        cmsg.setCertificateUuid(msg.getCertificateUuid());
        bus.makeLocalServiceId(cmsg, LoadBalancerConstants.SERVICE_ID);
        bus.send(cmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()){
                    LoadBalancerListenerInventory inv = LoadBalancerListenerInventory.valueOf(dbf.findByUuid(msg.getListenerUuid(), LoadBalancerListenerVO.class));
                    evt.setInventory(inv);
                    bus.publish(evt);
                } else {
                    dbf.remove(original_ref);
                    evt.setError(reply.getError());
                    bus.publish(evt);
                }
            }
        });
    }

    private void handle(APIRemoveCertificateFromLoadBalancerListenerMsg msg) {
        APIRemoveCertificateFromLoadBalancerListenerEvent evt = new APIRemoveCertificateFromLoadBalancerListenerEvent(msg.getId());

        LoadBalancerListenerCertificateRefVO ref = Q.New(LoadBalancerListenerCertificateRefVO.class)
                .eq(LoadBalancerListenerCertificateRefVO_.listenerUuid, msg.getListenerUuid())
                .eq(LoadBalancerListenerCertificateRefVO_.certificateUuid, msg.getCertificateUuid()).limit(1).find();
        final LoadBalancerListenerCertificateRefVO original_ref = ref;
        if (ref == null) {
            LoadBalancerListenerInventory inv = LoadBalancerListenerInventory.valueOf(dbf.findByUuid(msg.getListenerUuid(), LoadBalancerListenerVO.class));
            evt.setInventory(inv);
            bus.publish(evt);
            return;
        } else {
            dbf.remove(ref);
        }

        LoadBalancerChangeCertificateMsg cmsg = new LoadBalancerChangeCertificateMsg();
        cmsg.setListenerUuid(msg.getListenerUuid());
        cmsg.setLoadBalancerUuid(msg.getLoadBalancerUuid());
        cmsg.setCertificateUuid(msg.getCertificateUuid());
        bus.makeLocalServiceId(cmsg, LoadBalancerConstants.SERVICE_ID);
        bus.send(cmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()){
                    LoadBalancerListenerInventory inv = LoadBalancerListenerInventory.valueOf(dbf.findByUuid(msg.getListenerUuid(), LoadBalancerListenerVO.class));
                    evt.setInventory(inv);
                    bus.publish(evt);
                } else {
                    dbf.persist(original_ref);
                    evt.setError(reply.getError());
                    bus.publish(evt);
                }
            }
        });
    }

    private void handle(LoadBalancerChangeCertificateMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                LoadBalancerChangeCertificateReply reply = new LoadBalancerChangeCertificateReply();

                /* there is no vm nic, can not installed to backend
                 * it will be installed to backend when binding vm nic */
                if (self.getProviderType() == null) {
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                LoadBalancerBackend bkd = getBackend();
                LoadBalancerStruct s = makeStruct();
                s.setInit(false);
                bkd.refresh(s, new Completion(msg) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                    }
                });

                chain.next();
            }

            @Override
            public String getName() {
                return "change-lb-listener-certificate";
            }
        });
    }
}
