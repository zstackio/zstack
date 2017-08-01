package org.zstack.compute.host;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.*;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.defer.Deferred;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.SyncThread;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.AbstractService;
import org.zstack.header.allocator.HostCpuOverProvisioningManager;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.*;
import org.zstack.search.GetQuery;
import org.zstack.search.SearchQuery;
import org.zstack.tag.TagManager;
import org.zstack.utils.Bucket;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.ObjectUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.*;

import static org.zstack.core.Platform.operr;

public class HostManagerImpl extends AbstractService implements HostManager, ManagementNodeChangeListener,
        ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(HostManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private DbEntityLister dl;
    @Autowired
    private ResourceDestinationMaker destMaker;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private HostExtensionPointEmitter extEmitter;
    @Autowired
    protected HostTracker tracker;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private HostCpuOverProvisioningManager cpuRatioMgr;
    @Autowired
    private EventFacade evtf;

    private Map<Class, HostBaseExtensionFactory> hostBaseExtensionFactories = new HashMap<>();


    private Map<String, HypervisorFactory> hypervisorFactories = Collections.synchronizedMap(new HashMap<String, HypervisorFactory>());
    private static final Set<Class> allowedMessageAfterSoftDeletion = new HashSet<Class>();

    static {
        allowedMessageAfterSoftDeletion.add(HostDeletionMsg.class);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIAddHostMsg) {
            handle((APIAddHostMsg) msg);
        } else if (msg instanceof APIListHostMsg) {
            handle((APIListHostMsg) msg);
        } else if (msg instanceof APISearchHostMsg) {
            handle((APISearchHostMsg) msg);
        } else if (msg instanceof APIGetHostMsg) {
            handle((APIGetHostMsg) msg);
        } else if (msg instanceof APIGetHypervisorTypesMsg) {
            handle((APIGetHypervisorTypesMsg) msg);
        } else if (msg instanceof HostMessage) {
            HostMessage hmsg = (HostMessage) msg;
            passThrough(hmsg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetHypervisorTypesMsg msg) {
        APIGetHypervisorTypesReply reply = new APIGetHypervisorTypesReply();
        List<String> res = new ArrayList<String>();
        res.addAll(HypervisorType.getAllTypeNames());
        reply.setHypervisorTypes(res);
        bus.reply(msg, reply);
    }

    private void handle(APIGetHostMsg msg) {
        GetQuery q = new GetQuery();
        String res = q.getAsString(msg, HostInventory.class);
        APIGetHostReply reply = new APIGetHostReply();
        reply.setInventory(res);
        bus.reply(msg, reply);
    }

    private void handle(APISearchHostMsg msg) {
        SearchQuery<HostInventory> query = SearchQuery.create(msg, HostInventory.class);
        String content = query.listAsString();
        APISearchHostReply reply = new APISearchHostReply();
        reply.setContent(content);
        bus.reply(msg, reply);
    }

    private void handle(APIListHostMsg msg) {
        List<HostVO> vos = dl.listByApiMessage(msg, HostVO.class);
        List<HostInventory> invs = HostInventory.valueOf(vos);
        APIListHostReply reply = new APIListHostReply();
        reply.setInventories(invs);
        bus.reply(msg, reply);
    }

    private void passThrough(HostMessage msg) {
        HostVO vo = dbf.findByUuid(msg.getHostUuid(), HostVO.class);
        if (vo == null && allowedMessageAfterSoftDeletion.contains(msg.getClass())) {
            HostEO eo = dbf.findByUuid(msg.getHostUuid(), HostEO.class);
            vo = ObjectUtils.newAndCopy(eo, HostVO.class);
        }

        if (vo == null) {
            String err = "Cannot find host: " + msg.getHostUuid() + ", it may have been deleted";
            bus.replyErrorByMessageType((Message) msg, err);
            return;
        }

        HypervisorFactory factory = this.getHypervisorFactory(HypervisorType.valueOf(vo.getHypervisorType()));
        Host host = factory.getHost(vo);
        host.handleMessage((Message) msg);
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof HostMessage) {
            passThrough((HostMessage) msg);
        } else if (msg instanceof AddHostMsg){
            handle((AddHostMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private AddHostMsg getAddHostMsg(AddHostMessage msg) {
        if (msg instanceof AddHostMsg) {
            return (AddHostMsg) msg;
        } else if (msg instanceof APIAddHostMsg) {
            return AddHostMsg.valueOf((APIAddHostMsg) msg);
        }

        throw new CloudRuntimeException("unexpected addHost message: " + msg);
    }

    private void doAddHost(final AddHostMessage msg, ReturnValueCompletion<HostInventory > completion) {
        final ClusterVO cluster = findClusterByUuid(msg.getClusterUuid());
        final HostVO hvo = new HostVO();
        if (msg.getResourceUuid() != null) {
            hvo.setUuid(msg.getResourceUuid());
        } else {
            hvo.setUuid(Platform.getUuid());
        }
        hvo.setClusterUuid(cluster.getUuid());
        hvo.setZoneUuid(cluster.getZoneUuid());
        hvo.setName(msg.getName());
        hvo.setDescription(msg.getDescription());
        hvo.setHypervisorType(cluster.getHypervisorType());
        hvo.setManagementIp(msg.getManagementIp());
        hvo.setStatus(HostStatus.Connecting);
        hvo.setState(HostState.Enabled);

        final HypervisorFactory factory = getHypervisorFactory(HypervisorType.valueOf(cluster.getHypervisorType()));
        final HostVO vo = factory.createHost(hvo, msg);
        final AddHostMsg amsg = getAddHostMsg(msg);

        if (msg instanceof APIAddHostMsg) {
            tagMgr.createTagsFromAPICreateMessage((APIAddHostMsg)msg, vo.getUuid(), HostVO.class.getSimpleName());
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        final HostInventory inv = HostInventory.valueOf(vo);
        chain.setName(String.format("add-host-%s", vo.getUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "call-before-add-host-extension";

            private void callPlugins(final Iterator<HostAddExtensionPoint> it, final FlowTrigger trigger) {
                if (!it.hasNext()) {
                    trigger.next();
                    return;
                }

                HostAddExtensionPoint ext = it.next();
                ext.beforeAddHost(inv, new Completion(trigger) {
                    @Override
                    public void success() {
                        callPlugins(it, trigger);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                List<HostAddExtensionPoint> exts = pluginRgty.getExtensionList(HostAddExtensionPoint.class);
                callPlugins(exts.iterator(), trigger);
            }

        }).then(new NoRollbackFlow() {
            String __name__ = "send-connect-host-message";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                ConnectHostMsg connectMsg = new ConnectHostMsg(vo.getUuid());
                connectMsg.setNewAdd(true);
                connectMsg.setStartPingTaskOnFailure(false);
                bus.makeTargetServiceIdByResourceUuid(connectMsg, HostConstant.SERVICE_ID, hvo.getUuid());
                bus.send(connectMsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            trigger.next();
                        } else {
                            trigger.fail(reply.getError());
                        }
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "check-host-os-version";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                String distro = HostSystemTags.OS_DISTRIBUTION.getTokenByResourceUuid(vo.getUuid(), HostSystemTags.OS_DISTRIBUTION_TOKEN);
                String release = HostSystemTags.OS_RELEASE.getTokenByResourceUuid(vo.getUuid(), HostSystemTags.OS_RELEASE_TOKEN);
                String version = HostSystemTags.OS_VERSION.getTokenByResourceUuid(vo.getUuid(), HostSystemTags.OS_VERSION_TOKEN);

                if (distro == null && release == null && version == null) {
                    trigger.fail(operr("after connecting, host[name:%s, ip:%s] returns a null os version", vo.getName(), vo.getManagementIp()));
                    return;
                }

                SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
                q.select(HostVO_.uuid);
                q.add(HostVO_.clusterUuid, Op.EQ, vo.getClusterUuid());
                q.add(HostVO_.uuid, Op.NOT_EQ, vo.getUuid());
                q.add(HostVO_.status, Op.NOT_EQ, HostStatus.Connecting);
                q.setLimit(1);
                List<String> huuids = q.listValue();
                if (huuids.isEmpty()) {
                    // this the first host in cluster
                    trigger.next();
                    return;
                }

                String otherHostUuid = huuids.get(0);
                String cdistro = HostSystemTags.OS_DISTRIBUTION.getTokenByResourceUuid(otherHostUuid, HostSystemTags.OS_DISTRIBUTION_TOKEN);
                String crelease = HostSystemTags.OS_RELEASE.getTokenByResourceUuid(otherHostUuid, HostSystemTags.OS_RELEASE_TOKEN);
                String cversion = HostSystemTags.OS_VERSION.getTokenByResourceUuid(otherHostUuid, HostSystemTags.OS_VERSION_TOKEN);
                if (cdistro == null && crelease == null && cversion == null) {
                    // this the first host in cluster
                    trigger.next();
                    return;
                }

                if (version.contains(".")) {
                    version = version.split("\\.")[0];
                }

                if (cversion.contains(".")) {
                    cversion = cversion.split("\\.")[0];
                }

                String mineVersion = String.format("%s;%s;%s", distro, release, version);
                String currentVersion = String.format("%s;%s;%s", cdistro, crelease, cversion);

                if (!mineVersion.equals(currentVersion)) {
                    trigger.fail(operr("cluster[uuid:%s] already has host with os version[%s], but new added host[name:%s ip:%s] has host os version[%s]",
                            vo.getClusterUuid(), currentVersion, vo.getName(), vo.getManagementIp(), mineVersion));
                    return;
                }

                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "call-after-add-host-extension";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                extEmitter.afterAddHost(inv, new Completion(trigger) {
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
        }).done(new FlowDoneHandler(amsg) {
            @Override
            public void handle(Map data) {
                HostVO nvo = dbf.reload(vo);
                HostInventory inv = factory.getHostInventory(nvo.getUuid());
                inv.setStatus(HostStatus.Connected.toString());
                completion.success(inv);

                logger.debug(String.format("successfully added host[name:%s, hypervisor:%s, uuid:%s]", vo.getName(), vo.getHypervisorType(), vo.getUuid()));
            }
        }).error(new FlowErrorHandler(amsg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                // delete host totally through the database, so other tables
                // refer to the host table will clean up themselves
                HostVO nvo = dbf.reload(vo);
                dbf.remove(nvo);
                dbf.eoCleanup(HostVO.class);
                HostInventory inv = HostInventory.valueOf(nvo);

                CollectionUtils.safeForEach(pluginRgty.getExtensionList(FailToAddHostExtensionPoint.class), new ForEachFunction<FailToAddHostExtensionPoint>() {
                    @Override
                    public void run(FailToAddHostExtensionPoint ext) {
                        ext.failedToAddHost(inv, msg);
                    }
                });

                completion.fail(errf.instantiateErrorCode(HostErrors.UNABLE_TO_ADD_HOST, errCode));
            }
        }).start();

    }

    @Deferred
    private void handle(final AddHostMsg msg) {
        final AddHostReply reply = new AddHostReply();

        doAddHost(msg, new ReturnValueCompletion<HostInventory>(msg) {
            @Override
            public void success(HostInventory returnValue) {
                reply.setInventory(returnValue);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Deferred
    private void handle(final APIAddHostMsg msg) {
        final APIAddHostEvent evt = new APIAddHostEvent(msg.getId());

        doAddHost(msg, new ReturnValueCompletion<HostInventory>(msg) {
            @Override
            public void success(HostInventory inventory) {
                evt.setInventory(inventory);
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private ClusterVO findClusterByUuid(String uuid) {
        SimpleQuery<ClusterVO> query = dbf.createQuery(ClusterVO.class);
        query.add(ClusterVO_.uuid, Op.EQ, uuid);
        return query.find();
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(HostConstant.SERVICE_ID);
    }

    private void populateExtensions() {
        for (HypervisorFactory f : pluginRgty.getExtensionList(HypervisorFactory.class)) {
            HypervisorFactory old = hypervisorFactories.get(f.getHypervisorType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate HypervisorFactory[%s, %s] for hypervisor type[%s]",
                        old.getClass().getName(), f.getClass().getName(), f.getHypervisorType()));
            }
            hypervisorFactories.put(f.getHypervisorType().toString(), f);
        }

        for (HostBaseExtensionFactory ext : pluginRgty.getExtensionList(HostBaseExtensionFactory.class)) {
            for (Class clz : ext.getMessageClasses()) {
                HostBaseExtensionFactory old = hostBaseExtensionFactories.get(clz);
                if (old != null) {
                    throw new CloudRuntimeException(String.format("duplicate HostBaseExtensionFactory[%s, %s] for the" +
                            " message[%s]", old.getClass(), ext.getClass(), clz));
                }
                hostBaseExtensionFactories.put(clz, ext);
            }
        }

    }

    @Override
    public boolean start() {
        setupGlobalConfig();
        populateExtensions();
        setupCanonicalEvents();
        return true;
    }

    private void setupCanonicalEvents(){
        evtf.on(PrimaryStorageCanonicalEvent.PRIMARY_STORAGE_HOST_STATUS_CHANGED_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                PrimaryStorageCanonicalEvent.PrimaryStorageHostStatusChangeData d =
                        (PrimaryStorageCanonicalEvent.PrimaryStorageHostStatusChangeData)data;
                if (d.getNewStatus() == PrimaryStorageHostStatus.Disconnected &&
                        d.getOldStatus() != PrimaryStorageHostStatus.Disconnected &&
                        noStorageAccessible(d.getHostUuid())){
                    ChangeHostConnectionStateMsg msg = new ChangeHostConnectionStateMsg();
                    msg.setHostUuid(d.getHostUuid());
                    msg.setConnectionStateEvent(HostStatusEvent.disconnected.toString());
                    bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, d.getHostUuid());
                    bus.send(msg);
                }
            }
        });
    }

    @Transactional(readOnly = true)
    private boolean noStorageAccessible(String hostUuid){
        // detach ps will delete PrimaryStorageClusterRefVO first.
        List<String> attachedPsUuids = SQL.New("select distinct ref.primaryStorageUuid" +
                " from PrimaryStorageClusterRefVO ref, HostVO h" +
                " where h.uuid =:hostUuid" +
                " and ref.clusterUuid = h.clusterUuid", String.class)
                .param("hostUuid", hostUuid)
                .list();

        long attachedPsCount = attachedPsUuids.size();
        long inaccessiblePsCount = attachedPsCount == 0 ? 0 : Q.New(PrimaryStorageHostRefVO.class)
                .eq(PrimaryStorageHostRefVO_.hostUuid, hostUuid)
                .eq(PrimaryStorageHostRefVO_.status, PrimaryStorageHostStatus.Disconnected)
                .in(PrimaryStorageHostRefVO_.primaryStorageUuid, attachedPsUuids)
                .count();

        return inaccessiblePsCount == attachedPsCount && attachedPsCount > 0;
    }

    private void setupGlobalConfig() {
        HostGlobalConfig.HOST_CPU_OVER_PROVISIONING_RATIO.installLocalUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                cpuRatioMgr.setGlobalRatio(newConfig.value(Integer.class));
            }
        });
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void nodeJoin(String nodeId) {
    }

    @Override
    @SyncThread
    public void nodeLeft(String nodeId) {
        logger.debug(String.format("Management node[uuid:%s] left, node[uuid:%s] starts to take over hosts", nodeId, Platform.getManagementServerId()));
        loadHost();
    }

    @Override
    public void iAmDead(String nodeId) {
    }

    private Bucket getHostManagedByUs() {
        int qun = 10000;
        long amount = dbf.count(HostVO.class);
        int times = (int) (amount / qun) + (amount % qun != 0 ? 1 : 0);
        List<String> connected = new ArrayList<String>();
        List<String> disconnected = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i < times; i++) {
            SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
            q.select(HostVO_.uuid, HostVO_.status);
            q.setLimit(qun);
            q.setStart(start);
            List<Tuple> lst = q.listTuple();
            start += qun;
            for (Tuple t : lst) {
                String huuid = t.get(0, String.class);
                if (!destMaker.isManagedByUs(huuid)) {
                    continue;
                }
                HostStatus state = t.get(1, HostStatus.class);
                if (state == HostStatus.Connected) {
                    connected.add(huuid);
                } else {
                    // for Disconnected and Connecting, treat as Disconnected
                    disconnected.add(huuid);
                }
            }
        }

        return Bucket.newBucket(connected, disconnected);
    }


    private void loadHost() {
        Bucket hosts = getHostManagedByUs();
        List<String> connected = hosts.get(0);
        List<String> disconnected = hosts.get(1);
        List<String> hostsToLoad = new ArrayList<String>();

        if (CoreGlobalProperty.UNIT_TEST_ON) {
            hostsToLoad.addAll(connected);
            hostsToLoad.addAll(disconnected);
        } else {
            if (HostGlobalConfig.RECONNECT_ALL_ON_BOOT.value(Boolean.class)) {
                hostsToLoad.addAll(connected);
                hostsToLoad.addAll(disconnected);
            } else {
                hostsToLoad.addAll(disconnected);
                tracker.trackHost(connected);
            }
        }

        if (hostsToLoad.isEmpty()) {
            return;
        }

        String serviceId = bus.makeLocalServiceId(HostConstant.SERVICE_ID);
        final List<ConnectHostMsg> msgs = new ArrayList<ConnectHostMsg>(hostsToLoad.size());
        for (String uuid : hostsToLoad) {
            ConnectHostMsg connectMsg = new ConnectHostMsg(uuid);
            connectMsg.setNewAdd(false);
            connectMsg.setServiceId(serviceId);
            connectMsg.setStartPingTaskOnFailure(true);
            msgs.add(connectMsg);
        }

        bus.send(msgs, HostGlobalConfig.HOST_LOAD_PARALLELISM_DEGREE.value(Integer.class),
                new CloudBusSteppingCallback(null) {
            @Override
            public void run(NeedReplyMessage msg, MessageReply reply) {
                ConnectHostMsg cmsg = (ConnectHostMsg) msg;
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to load host[uuid:%s], %s", cmsg.getHostUuid(), reply.getError()));
                } else {
                    logger.debug(String.format("host[uuid:%s] load successfully", cmsg.getHostUuid()));
                }
            }
        });
    }

    @Override
    public void iJoin(String nodeId) {
    }


    public HypervisorFactory getHypervisorFactory(HypervisorType type) {
        HypervisorFactory factory = hypervisorFactories.get(type.toString());
        if (factory == null) {
            throw new CloudRuntimeException("No factory for hypervisor: " + type + " found, check your HypervisorManager.xml");
        }

        return factory;
    }

    @Override
    @AsyncThread
    public void managementNodeReady() {
        logger.debug(String.format("Management node[uuid:%s] joins, start loading host...", Platform.getManagementServerId()));
        loadHost();
    }

    @Override
    public HostBaseExtensionFactory getHostBaseExtensionFactory(Message msg) {
        return hostBaseExtensionFactories.get(msg.getClass());
    }
}
