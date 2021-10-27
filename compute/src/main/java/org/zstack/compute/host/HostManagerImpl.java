package org.zstack.compute.host;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.*;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.defer.Deferred;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.*;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.AbstractService;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.allocator.HostCpuOverProvisioningManager;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.managementnode.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageCanonicalEvent;
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO;
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO_;
import org.zstack.header.storage.primary.PrimaryStorageHostStatus;
import org.zstack.header.vo.FindSameNodeExtensionPoint;
import org.zstack.header.vo.ResourceInventory;
import org.zstack.header.zone.ZoneVO;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.tag.TagManager;
import org.zstack.utils.Bucket;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.ObjectUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.Pair;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;
import static org.zstack.longjob.LongJobUtils.noncancelableErr;

public class HostManagerImpl extends AbstractService implements HostManager, ManagementNodeChangeListener,
        ManagementNodeReadyExtensionPoint, FindSameNodeExtensionPoint {
    private static final CLogger logger = Utils.getLogger(HostManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
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
    @Autowired
    private ResourceConfigFacade rcf;

    private Map<Class, HostBaseExtensionFactory> hostBaseExtensionFactories = new HashMap<>();
    private List<HostExtensionManager> hostExtensionManagers = new ArrayList<>();
    private List<HostPriorityCaculator> hostPriorityCaculators = new ArrayList<>();

    private Map<String, HypervisorFactory> hypervisorFactories = Collections.synchronizedMap(new HashMap<String, HypervisorFactory>());
    private static final Set<Class> allowedMessageAfterSoftDeletion = new HashSet<Class>();
    private Future reportHostCapacityTask;

    static {
        allowedMessageAfterSoftDeletion.add(HostDeletionMsg.class);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIAddHostMsg) {
            handle((APIAddHostMsg) msg);
        } else if (msg instanceof APIGetHypervisorTypesMsg) {
            handle((APIGetHypervisorTypesMsg) msg);
        } else if (msg instanceof APIGetHostTaskMsg) {
            handle((APIGetHostTaskMsg) msg);
        } else if (msg instanceof HostMessage) {
            HostMessage hmsg = (HostMessage) msg;
            passThrough(hmsg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetHypervisorTypesMsg msg) {
        APIGetHypervisorTypesReply reply = new APIGetHypervisorTypesReply();
        List<String> res = new ArrayList<>(HypervisorType.getAllTypeNames());
        reply.setHypervisorTypes(res);
        bus.reply(msg, reply);
    }

    private void handle(APIGetHostTaskMsg msg) {
        APIGetHostTaskReply reply = new APIGetHostTaskReply();
        Map<String, List<String>> mnIds = msg.getHostUuids().stream().collect(
                Collectors.groupingBy(huuid -> destMaker.makeDestination(huuid))
        );
        
        new While<>(mnIds.entrySet()).all((e, compl) -> {
            GetHostTaskMsg gmsg = new GetHostTaskMsg();
            gmsg.setHostUuids(e.getValue());
            bus.makeServiceIdByManagementNodeId(gmsg, HostConstant.SERVICE_ID, e.getKey());
            bus.send(gmsg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply r) {
                    if (r.isSuccess()) {
                        GetHostTaskReply gr = r.castReply();
                        reply.getResults().putAll(gr.getResults());
                    } else {
                        logger.error("get host task fail, because " + r.getError().getDetails());
                    }

                    compl.done();
                }
            });

        }).run(new NoErrorCompletion(msg) {
            @Override
            public void done() {
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(GetHostTaskMsg msg) {
        GetHostTaskReply reply = new GetHostTaskReply();
        List<HostVO> vos = Q.New(HostVO.class).in(HostVO_.uuid, msg.getHostUuids()).list();
        vos.forEach(vo -> {
            HypervisorFactory factory = this.getHypervisorFactory(HypervisorType.valueOf(vo.getHypervisorType()));
            Host host = factory.getHost(vo);
            reply.putResults(vo.getUuid(), thdf.getChainTaskInfo(host.getId()));
        });
        bus.reply(msg, reply);
    }

    private void passThrough(HostMessage msg) {
        HostVO vo = dbf.findByUuid(msg.getHostUuid(), HostVO.class);
        if (vo == null && allowedMessageAfterSoftDeletion.contains(msg.getClass())) {
            HostEO eo = dbf.findByUuid(msg.getHostUuid(), HostEO.class);
            vo = ObjectUtils.newAndCopy(eo, HostVO.class);
        }

        if (vo == null) {
            ErrorCode err = Platform.err(SysErrors.RESOURCE_NOT_FOUND, "cannot find host[uuid:%s], it may have been deleted", msg.getHostUuid());
            throw new OperationFailureException(err);
        }

        HypervisorFactory factory = this.getHypervisorFactory(HypervisorType.valueOf(vo.getHypervisorType()));
        Host host = factory.getHost(vo);
        host.handleMessage((Message) msg);
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        HostExtensionManager extensionManager = hostExtensionManagers.stream().filter(it -> it.getMessageClasses()
                .stream().anyMatch(clz -> clz.isAssignableFrom(msg.getClass()))).findFirst().orElse(null);
        if (extensionManager != null) {
            extensionManager.handleMessage(msg);
        } else if (msg instanceof APIMessage) {
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
        } else if (msg instanceof GetHostTaskMsg) {
            handle((GetHostTaskMsg) msg);
        } else if (msg instanceof CancelHostTasksMsg) {
            handle((CancelHostTasksMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void addHostInQueue(final AddHostMessage msg, ReturnValueCompletion<HostInventory> completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return "batch-add-host";
            }

            @Override
            public void run(SyncTaskChain chain) {
                doAddHostInQueue(msg, new ReturnValueCompletion<HostInventory>(completion) {
                    @Override
                    public void success(HostInventory returnValue) {
                        completion.success(returnValue);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }

            @Override
            protected int getSyncLevel() {
                return ThreadGlobalProperty.MAX_THREAD_NUM / 5;
            }
        });
    }

    private void doAddHostInQueue(final AddHostMessage msg, ReturnValueCompletion<HostInventory> completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return String.format("add-host-%s", msg.getManagementIp());
            }

            @Override
            public void run(final SyncTaskChain chain) {
                doAddHost(msg, new ReturnValueCompletion<HostInventory>(completion, chain) {
                    @Override
                    public void success(HostInventory returnValue) {
                        completion.success(returnValue);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
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

    private void doAddHost(final AddHostMessage msg, ReturnValueCompletion<HostInventory > completion) {
        if (Q.New(HostVO.class).eq(HostVO_.managementIp, msg.getManagementIp()).isExists()) {
            completion.fail(argerr("there has been a host having managementIp[%s]", msg.getManagementIp()));
            return;
        }

        final ClusterVO cluster = findClusterByUuid(msg.getClusterUuid());
        if (cluster == null) {
            completion.fail(argerr("cluster[uuid:%s] is not existing", msg.getClusterUuid()));
            return;
        }

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

        if (msg instanceof APIAddHostMsg) {
            tagMgr.createTagsFromAPICreateMessage((APIAddHostMsg)msg, vo.getUuid(), HostVO.class.getSimpleName());
        } else if (msg instanceof AddHostMsg) {
            tagMgr.createTags(((AddHostMsg) msg).getSystemTags(), ((AddHostMsg) msg).getUserTags(), vo.getUuid(), HostVO.class.getSimpleName());
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
            @Override
            public void run(FlowTrigger trigger, Map data) {
                String arch = HostSystemTags.CPU_ARCHITECTURE.getTokenByResourceUuid(vo.getUuid(), HostSystemTags.CPU_ARCHITECTURE_TOKEN);

                if (arch == null) {
                    trigger.fail(operr("after connecting, host[name:%s, ip:%s] returns a null architecture", vo.getName(), vo.getManagementIp()));
                    return;
                }

                ClusterVO cluster = dbf.findByUuid(msg.getClusterUuid(), ClusterVO.class);
                if (cluster.getArchitecture() == null) {
                    cluster.setArchitecture(arch);
                    dbf.update(cluster);
                    trigger.next();
                    return;
                }

                if (!arch.equals(cluster.getArchitecture())) {
                    trigger.fail(operr("cluster[uuid:%s]'s architecture is %s, not match the host[name:%s, ip:%s] architecture %s",
                            vo.getClusterUuid(), cluster.getArchitecture(), vo.getName(), vo.getManagementIp(), arch));
                    return;
                }

                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "check-host-os-version";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                String distro = HostSystemTags.OS_DISTRIBUTION.getTokenByResourceUuid(vo.getUuid(), HostSystemTags.OS_DISTRIBUTION_TOKEN);
                String release = HostSystemTags.OS_RELEASE.getTokenByResourceUuid(vo.getUuid(), HostSystemTags.OS_RELEASE_TOKEN);
                String version = HostSystemTags.OS_VERSION.getTokenByResourceUuid(vo.getUuid(), HostSystemTags.OS_VERSION_TOKEN);
                String arch = HostSystemTags.CPU_ARCHITECTURE.getTokenByResourceUuid(vo.getUuid(), HostSystemTags.CPU_ARCHITECTURE_TOKEN);

                if (distro == null || release == null || version == null) {
                    trigger.fail(operr("after connecting, host[name:%s, ip:%s] returns a null os version", vo.getName(), vo.getManagementIp()));
                    return;
                }

                if (arch == null) {
                    trigger.fail(operr("after connecting, host[name:%s, ip:%s] returns a null architecture", vo.getName(), vo.getManagementIp()));
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
                if (cdistro == null || crelease == null || cversion == null) {
                    // this the first host in cluster
                    trigger.next();
                    return;
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
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                HostInventory inv = factory.getHostInventory(vo.getUuid());
                logger.debug(String.format("successfully added host[name:%s, hypervisor:%s, uuid:%s]", vo.getName(), vo.getHypervisorType(), vo.getUuid()));
                completion.success(inv);
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                // delete host totally through the database, so other tables
                // refer to the host table will clean up themselves
                HostVO nvo = dbf.reload(vo);
                dbf.remove(nvo);
                dbf.eoCleanup(HostVO.class, nvo.getUuid());
                HostInventory inv = HostInventory.valueOf(nvo);

                CollectionUtils.safeForEach(pluginRgty.getExtensionList(FailToAddHostExtensionPoint.class), new ForEachFunction<FailToAddHostExtensionPoint>() {
                    @Override
                    public void run(FailToAddHostExtensionPoint ext) {
                        ext.failedToAddHost(inv, msg);
                    }
                });

                completion.fail(errCode);
            }
        }).start();

    }

    @Deferred
    private void handle(final AddHostMsg msg) {
        final AddHostReply reply = new AddHostReply();

        addHostInQueue(msg, new ReturnValueCompletion<HostInventory>(msg) {
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

        addHostInQueue(msg, new ReturnValueCompletion<HostInventory>(msg) {
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

    private void handle(CancelHostTasksMsg msg) {
        CancelHostsTaskReply reply = new CancelHostsTaskReply();
        Set<String> runningSigs = thdf.getApiRunningTaskSignature(msg.getCancellationApiId());
        msg.addSearchedMnId(Platform.getManagementServerId());
        if (runningSigs != null) {
            runningSigs.forEach(id -> Optional.ofNullable(Host.getUuidFromId(id)).ifPresent(msg::addHostUuid));
        }

        List<String> restMnIds = Q.New(ManagementNodeVO.class).select(ManagementNodeVO_.uuid)
                .notIn(ManagementNodeVO_.uuid, msg.getSearchedMnIds())
                .listValues();
        if (!restMnIds.isEmpty()) {
            CancelHostTasksMsg nmsg = new CancelHostTasksMsg();
            nmsg.setHostUuids(msg.getHostUuids());
            nmsg.setSearchedMnIds(msg.getSearchedMnIds());
            nmsg.setCancellationApiId(msg.getCancellationApiId());
            bus.makeServiceIdByManagementNodeId(nmsg, HostConstant.SERVICE_ID, restMnIds.get(0));
            bus.send(nmsg, new CloudBusCallBack(msg) {
                @Override
                public void run(MessageReply r) {
                    if (!r.isSuccess()) {
                        reply.setError(r.getError());
                    }
                    bus.reply(msg, reply);
                }
            });
            return;
        }

        if (msg.getHostUuids().isEmpty()) {
            reply.setError(noncancelableErr(i18n("no running api[%s] task on hosts", msg.getCancellationApiId())));
            bus.reply(msg, reply);
            return;
        }

        ErrorCodeList err = new ErrorCodeList();
        new While<>(msg.getHostUuids()).all((hostUuid, compl) -> {
            CancelHostTaskMsg cmsg = new CancelHostTaskMsg();
            cmsg.setHostUuid(hostUuid);
            cmsg.setCancellationApiId(msg.getCancellationApiId());
            bus.makeLocalServiceId(cmsg, HostConstant.SERVICE_ID);
            bus.send(cmsg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        err.getCauses().add(reply.getError());
                    }
                    compl.done();
                }
            });
        }).run(new NoErrorCompletion(msg) {
            @Override
            public void done() {
                if (!err.getCauses().isEmpty()) {
                    reply.setError(err.getCauses().get(0));
                }

                bus.reply(msg, reply);
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

        hostExtensionManagers.addAll(pluginRgty.getExtensionList(HostExtensionManager.class));
        hostPriorityCaculators.addAll(pluginRgty.getExtensionList(HostPriorityCaculator.class));
    }

    @Override
    public boolean start() {
        setupGlobalConfig();
        populateExtensions();
        setupCanonicalEvents();
        startPeriodTasks();
        return true;
    }

    private void startPeriodTasks() {
        HostGlobalConfig.REPORT_HOST_CAPACITY_INTERVAL.installUpdateExtension((oldConfig, newConfig) -> startReportHostCapacityTask());
        startReportHostCapacityTask();
    }

    private void startReportHostCapacityTask() {
        if (reportHostCapacityTask != null) {
            reportHostCapacityTask.cancel(true);
        }
        reportHostCapacityTask = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.MINUTES;
            }

            @Override
            public long getInterval() {
                return getReportInterval();
            }

            @Override
            public String getName() {
                return "report-host-capacity-task";
            }

            @Override
            public void run() {
                reportHostCapacity();
            }
        });
    }

    private void reportHostCapacity() {
        List<String> hostUuids = Q.New(HostVO.class)
                .select(HostVO_.uuid)
                .eq(HostVO_.status, HostStatus.Connected)
                .listValues();

        if (hostUuids.isEmpty()) {
            return;
        }

        new While<>(hostUuids).step((hostUuid, completion) -> {
            CheckHostCapacityMsg msg = new CheckHostCapacityMsg();
            msg.setHostUuid(hostUuid);
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
            bus.send(msg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply rly) {
                    if (!rly.isSuccess()) {
                        logger.warn(String.format("failed to get capacity on the host[uuid:%s], %s",
                                hostUuid, rly.getError().toString()));
                    }
                    RecalculateHostCapacityMsg rmsg = new RecalculateHostCapacityMsg();
                    rmsg.setHostUuid(hostUuid);
                    bus.makeLocalServiceId(rmsg, HostAllocatorConstant.SERVICE_ID);
                    bus.send(rmsg);
                    completion.done();
                }
            });
        }, 15).run(new NoErrorCompletion() {
            @Override
            public void done() {
                // do nothing
            }
        });
    }

    private int getReportInterval() {
        return HostGlobalConfig.REPORT_HOST_CAPACITY_INTERVAL.value(Integer.class);
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
                    msg.setCause("base cause: host disconnected from other status and has no connected primary storage attached");
                    bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, d.getHostUuid());
                    bus.send(msg);

                    new HostBase.HostDisconnectedCanonicalEvent(d.getHostUuid(),
                            operr("primary storage[uuid:%s] becomes disconnected, the host has no connected primary storage attached",
                                    d.getPrimaryStorageUuid())).fire();
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
        HostGlobalConfig.HOST_CPU_OVER_PROVISIONING_RATIO.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                cpuRatioMgr.setGlobalRatio(newConfig.value(Integer.class));
            }
        });

        ResourceConfig cpuConfig = rcf.getResourceConfig(HostGlobalConfig.HOST_CPU_OVER_PROVISIONING_RATIO.getIdentity());
        cpuConfig.installLocalUpdateExtension((config, resourceUuid, resourceType, oldValue, newValue) ->
                recalculateHostCapacity(resourceUuid, resourceType));
        cpuConfig.installLocalDeleteExtension((config, resourceUuid, resourceType, originValue) ->
                recalculateHostCapacity(resourceUuid, resourceType));
    }

    private void recalculateHostCapacity(String resourceUuid, String resourceType) {
        RecalculateHostCapacityMsg msg = new RecalculateHostCapacityMsg();
        bus.makeTargetServiceIdByResourceUuid(msg, HostAllocatorConstant.SERVICE_ID, resourceUuid);
        if (resourceType.equals(ZoneVO.class.getSimpleName())) {
            msg.setZoneUuid(resourceUuid);
        } else if (resourceType.equals(ClusterVO.class.getSimpleName())) {
            msg.setClusterUuid(resourceUuid);
        } else if (resourceType.equals(HostVO.class.getSimpleName())) {
            msg.setHostUuid(resourceUuid);
        }
        bus.send(msg);
    }

    @Override
    public boolean stop() {
        if (reportHostCapacityTask != null) {
            reportHostCapacityTask.cancel(true);
        }

        return true;
    }

    @Override
    public void nodeJoin(ManagementNodeInventory inv) {
    }

    @Override
    @SyncThread
    public void nodeLeft(ManagementNodeInventory inv) {
        logger.debug(String.format("Management node[uuid:%s] left, node[uuid:%s] starts to take over hosts", inv.getUuid(), Platform.getManagementServerId()));
        loadHost(true);
    }

    @Override
    public void iAmDead(ManagementNodeInventory inv) {
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

    private int getHostConnectPriority(String hostUuid) {
        int priority = 0;

        for (HostPriorityCaculator c : hostPriorityCaculators) {
            int p = c.getHostConnectPriority(hostUuid);
            if (p > priority) {
                priority = p;
            }
        }

        return priority;
    }

    private List<String> sortWithPriority(final List<String> hosts) {
        if (hostPriorityCaculators.isEmpty()) {
            return hosts;
        }

        List<Pair<String, Integer>> pairs = new ArrayList<>(hosts.size());
        for (String hostUuid: hosts) {
            pairs.add(new Pair<>(hostUuid, getHostConnectPriority(hostUuid)));
        }

        pairs.sort((p1, p2) -> {
            return p2.second() - p1.second(); // descend
        });

        return pairs.stream()
                .map(p -> p.first())
                .collect(Collectors.toList());
    }

    private void loadHost(boolean skipConnected) {
        Bucket hosts = getHostManagedByUs();
        List<String> connected = hosts.get(0);
        List<String> disconnected = hosts.get(1);
        List<String> hostsToLoad = new ArrayList<>();

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

        if (skipConnected) {
            hostsToLoad.removeAll(connected);
        }

        if (hostsToLoad.isEmpty()) {
            return;
        }

        final List<String> hostsToLoadSorted = sortWithPriority(hostsToLoad);
        logger.info("first host to load: " + hostsToLoadSorted.get(0));

        String serviceId = bus.makeLocalServiceId(HostConstant.SERVICE_ID);
        final List<ConnectHostMsg> msgs = new ArrayList<ConnectHostMsg>(hostsToLoad.size());
        for (String uuid : hostsToLoadSorted) {
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
                if (reply.isSuccess()) {
                    logger.debug(String.format("host[uuid:%s] load successfully", cmsg.getHostUuid()));
                } else if (reply.isCanceled()) {
                    logger.warn(String.format("canceled connect kvm host[uuid:%s], because it connecting now", cmsg.getHostUuid()));
                } else {
                    logger.warn(String.format("failed to load host[uuid:%s], %s", cmsg.getHostUuid(), reply.getError()));
                }
            }
        });
    }

    @Override
    public void iJoin(ManagementNodeInventory inv) {
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

        // Disconnected and connecting are not expected host status for ZStack
        // Need to reconnect those hosts when the node started.
        loadHost(true);
    }

    @Override
    public HostBaseExtensionFactory getHostBaseExtensionFactory(Message msg) {
        return hostBaseExtensionFactories.get(msg.getClass());
    }

    @Override
    public ResourceInventory findSameNode(String hostname) {
        String uuid = Q.New(HostVO.class).eq(HostVO_.managementIp, hostname).select(HostVO_.uuid).findValue();
        if (uuid == null) {
            return null;
        } else {
            ResourceInventory info = new ResourceInventory();
            info.setUuid(uuid);
            info.setResourceType(HostVO.class.getSimpleName());
            return info;
        }
    }
}
