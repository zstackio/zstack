package org.zstack.storage.zbs;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.cbd.kvm.CbdHeartbeatVolumeTO;
import org.zstack.cbd.kvm.CbdVolumeTo;
import org.zstack.vhost.kvm.VhostVolumeTO;
import org.zstack.compute.host.HostGlobalConfig;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.ansible.AnsibleGlobalProperty;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.storage.addon.primary.PrimaryStorageOutputProtocolRefVO;
import org.zstack.header.storage.addon.primary.PrimaryStorageOutputProtocolRefVO_;
import org.zstack.core.db.SQL;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.HasThreadContext;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.core.*;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.host.HostAO_;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostInventory;
import org.zstack.header.log.NoLogging;
import org.zstack.header.host.HostVO;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.addon.*;
import org.zstack.header.storage.addon.primary.*;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.VolumeSnapshotStats;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeProtocol;
import org.zstack.header.volume.VolumeStats;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.kvm.KVMHostVO;
import org.zstack.kvm.KVMHostVO_;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.storage.volume.VolumeGlobalConfig;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.storage.zbs.ZbsConstants.MDS_PING_FAIL_CYCLE_THRESHOLD;
import static org.zstack.storage.zbs.ZbsHelper.*;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

/**
 * @author Xingwei Yu
 * @date 2024/3/21 13:11
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ZbsStorageController implements PrimaryStorageControllerSvc, PrimaryStorageNodeSvc {
    private static final CLogger logger = Utils.getLogger(ZbsStorageController.class);

    @Autowired
    @Deprecated
    private DatabaseFacade dbf;
    @Autowired
    @Deprecated
    private ResourceConfigFacade rcf;
    @Autowired
    @Deprecated
    private CloudBus bus;

    private ExternalPrimaryStorageVO self;
    private AddonInfo addonInfo;
    private Config config;

    private Map<String, String> physicalPoolByLogicalPool = new ConcurrentHashMap<>();
    private final Map<MdsInfo, Integer> mdsConsecutivePingFailureCount = new ConcurrentHashMap<>();

    public static final String DEPLOY_CLIENT_PATH = "/zbs/primarystorage/client/deploy";
    public static final String GET_CAPACITY_PATH = "/zbs/primarystorage/capacity";
    public static final String GET_FACTS_PATH = "/zbs/primarystorage/facts";
    public static final String COPY_PATH = "/zbs/primarystorage/copy";
    public static final String CREATE_VOLUME_PATH = "/zbs/primarystorage/volume/create";
    public static final String DELETE_VOLUME_PATH = "/zbs/primarystorage/volume/delete";
    public static final String CLONE_VOLUME_PATH = "/zbs/primarystorage/volume/clone";
    public static final String QUERY_VOLUME_PATH = "/zbs/primarystorage/volume/query";
    public static final String BATCH_QUERY_VOLUME_PATH = "/zbs/primarystorage/volume/query/batch";
    public static final String EXPAND_VOLUME_PATH = "/zbs/primarystorage/volume/expand";
    public static final String FLATTEN_VOLUME_PATH = "/zbs/primarystorage/volume/flatten";
    public static final String CBD_TO_NBD_PATH = "/zbs/primarystorage/volume/cbdtonbd";
    public static final String CLEAN_NBD_PATH = "/zbs/primarystorage/volume/cleannbd";
    public static final String CREATE_SNAPSHOT_PATH = "/zbs/primarystorage/snapshot/create";
    public static final String DELETE_SNAPSHOT_PATH = "/zbs/primarystorage/snapshot/delete";
    public static final String ROLLBACK_SNAPSHOT_PATH = "/zbs/primarystorage/snapshot/rollback";
    public static final String CHECK_HOST_STORAGE_CONNECTION_PATH = "/zbs/primarystorage/check/host/connection";
    public static final String GET_VOLUME_CLIENTS_PATH = "/zbs/primarystorage/volume/clients";
    public static final String UPDATE_HOST_DEPENDENCY_PATH = "/zbs/primarystorage/host/updatedependency";
    public static final String VHOST_TARGET_HEALTH_PATH = "/zbs/primarystorage/vhost/target/health";
    public static final String PREPARE_VHOST_TARGET_ENV_PATH = "/zbs/primarystorage/vhost/target/prepareenv";
    public static final String VHOST_RESIZE_PATH = "/zbs/primarystorage/vhost/resize";
    public static final String DEPLOY_VHOST_PATH = "/zbs/primarystorage/vhost/deploy";
    public static final String DESTROY_VHOST_PATH = "/zbs/primarystorage/vhost/destroy";
    public static final String CREATE_VHOST_BDEV_PATH = "/zbs/primarystorage/vhost/bdev/create";
    public static final String DELETE_VHOST_BDEV_PATH = "/zbs/primarystorage/vhost/bdev/delete";

    private static final StorageCapabilities capabilities = new StorageCapabilities();

    static {
        VolumeSnapshotCapability scap = new VolumeSnapshotCapability();
        scap.setSupport(true);
        scap.setArrangementType(VolumeSnapshotCapability.VolumeSnapshotArrangementType.INDIVIDUAL);
        scap.setPlacementType(VolumeSnapshotCapability.VolumeSnapshotPlacementType.INTERNAL);
        scap.setSupportCreateOnHypervisor(false);
        scap.setSupportLazyDelete(false);
        scap.setVolumePathFromInternalSnapshotRegex("^[^@]+");
        capabilities.setSnapshotCapability(scap);
        capabilities.setSupportShareableVolume(true);
        capabilities.setSupportCloneFromVolume(false);
        capabilities.setSupportCloneFromAnotherSpace(false);
        capabilities.setSupportStorageQos(false);
        capabilities.setSupportLiveExpandVolume(false);
        capabilities.setSupportMultiSpace(true);
        capabilities.setSupportedImageFormats(Collections.singletonList(ImageConstant.RAW_FORMAT_STRING));
        capabilities.setDefaultIsoActiveProtocol(VolumeProtocol.CBD);
        capabilities.setDefaultImageExportProtocol(VolumeProtocol.NBD);
    }

    @Override
    public void activate(BaseVolumeInfo v, HostInventory h, boolean shareable, ReturnValueCompletion<ActiveVolumeTO> comp) {
        if (VolumeProtocol.Vhost.toString().equals(v.getProtocol())) {
            activateVhostVolume(v.getInstallPath(), h, comp);
            return;
        }

        if (VolumeProtocol.CBD.toString().equals(v.getProtocol())) {

            comp.success(new CbdVolumeTo());
            return;
        }

        comp.fail(operr(ORG_ZSTACK_STORAGE_ZBS_10006, "not supported protocol[%s]", v.getProtocol()));
    }

    private void activateVhostVolume(String installPath, HostInventory h, ReturnValueCompletion<ActiveVolumeTO> comp) {
        KVMHostVO host = getKvmHost(h);
        if (host == null) {
            comp.fail(operr(ORG_ZSTACK_STORAGE_ZBS_10010, "cannot find kvm host[uuid:%s], unable to activate vhost volume", h.getUuid()));
            return;
        }

        CreateVhostBdevCmd cmd = new CreateVhostBdevCmd();
        fillVhostHostParams(cmd, h, host);
        String relativePath = stripScheme(installPath);
        cmd.logicalPool = ZbsHelper.getPoolFromVolumePath(installPath);
        cmd.volume = relativePath.substring(relativePath.indexOf('/') + 1);
        cmd.bdevName = buildVhostBdevName(installPath);

        httpCall(CREATE_VHOST_BDEV_PATH, cmd, CreateVhostBdevRsp.class, new ReturnValueCompletion<CreateVhostBdevRsp>(comp) {
            @Override
            public void success(CreateVhostBdevRsp rsp) {
                VhostVolumeTO to = new VhostVolumeTO();
                to.setInstallPath(rsp.socketPath);
                comp.success(to);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.fail(errorCode);
            }
        });
    }

    private static String stripScheme(String installPath) {
        return installPath.replaceFirst(ZbsConstants.SCHEME_PREFIX, "");
    }

    private static String buildVhostBdevName(String installPath) {
        return ZbsConstants.VHOST_BDEV_NAME_PREFIX + installPathHash(installPath);
    }

    private static String installPathHash(String installPath) {
        return UUID.nameUUIDFromBytes(stripScheme(installPath).getBytes()).toString().replace("-", "");
    }

    private static String buildVhostSocketPath(String installPath) {
        return ZbsConstants.VHOST_SOCKET_DIR + "/" + buildVhostBdevName(installPath);
    }

    private KVMHostVO getKvmHost(HostInventory h) {
        return Q.New(KVMHostVO.class).eq(KVMHostVO_.uuid, h.getUuid()).find();
    }

    private void fillVhostHostParams(VhostHostCmd cmd, HostInventory h, KVMHostVO host) {
        cmd.hostIp = h.getManagementIp();
        cmd.sshPort = host.getPort();
        cmd.sshUsername = host.getUsername();
        cmd.sshPassword = host.getPassword();
    }

    @Override
    public void deactivate(String installPath, String protocol, HostInventory h, Completion comp) {
        if (VolumeProtocol.Vhost.toString().equals(protocol)) {
            KVMHostVO host = getKvmHost(h);
            if (host == null) {
                comp.fail(operr(ORG_ZSTACK_STORAGE_ZBS_10010, "cannot find kvm host[uuid:%s], unable to deactivate vhost volume", h.getUuid()));
                return;
            }

            DeleteVhostBdevCmd cmd = new DeleteVhostBdevCmd();
            fillVhostHostParams(cmd, h, host);
            cmd.bdevName = buildVhostBdevName(installPath);

            httpCall(DELETE_VHOST_BDEV_PATH, cmd, AgentResponse.class, new ReturnValueCompletion<AgentResponse>(comp) {
                @Override
                public void success(AgentResponse rsp) {
                    comp.success();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    comp.fail(errorCode);
                }
            });
            return;
        }

        // not support inactive client yet
        comp.success();
    }

    @Override
    public void deactivate(String installPath, String protocol, ActiveVolumeClient client, Completion comp) {
        comp.success();
    }

    @Override
    public void blacklist(String installPath, String protocol, HostInventory h) {
    }

    @Override
    public String getActivePath(BaseVolumeInfo v, HostInventory h, boolean shareable) {
        if (VolumeProtocol.Vhost.toString().equals(v.getProtocol())) {
            return buildVhostSocketPath(v.getInstallPath());
        }
        if (VolumeProtocol.CBD.toString().equals(v.getProtocol())) {
            return convertZbsPathToCbdPath(v.getInstallPath(), this::getPhysicalPoolName);
        } else {
            throw new OperationFailureException(operr(ORG_ZSTACK_STORAGE_ZBS_10007, "not supported protocol[%s] for active", v.getProtocol()));
        }
    }

    @Override
    public BaseVolumeInfo getActiveVolumeInfo(String activePath, HostInventory h, boolean shareable) {
        return null;
    }

    @Override
    public List<ActiveVolumeClient> getActiveClients(String installPath, String protocol) {
        if (!VolumeProtocol.CBD.toString().equals(protocol)
                && !VolumeProtocol.Vhost.toString().equals(protocol)) {
            throw new OperationFailureException(operr(ORG_ZSTACK_STORAGE_ZBS_10009, "not supported protocol[%s] for active", protocol));
        }

        GetVolumeClientsCmd cmd = new GetVolumeClientsCmd();
        cmd.setPath(installPath);
        GetVolumeClientsRsp rsp = new HttpCaller<>(GET_VOLUME_CLIENTS_PATH, cmd, GetVolumeClientsRsp.class,
                null, TimeUnit.SECONDS, 30, true)
                .setTryNext(true)
                .syncCall();
        List<ActiveVolumeClient> clients = new ArrayList<>();

        if (!rsp.isSuccess()) {
            throw new OperationFailureException(operr(ORG_ZSTACK_STORAGE_ZBS_10008, rsp.getError()));
        }

        if (rsp.getClients() != null) {
            for (ClientInfo clientInfo : rsp.getClients()) {
                ActiveVolumeClient client = new ActiveVolumeClient();
                client.setManagerIp(clientInfo.getIp());
                clients.add(client);
            }
        }
        return clients;
    }

    @Override
    public List<String> getActiveVolumesLocation(HostInventory h) {
        return null;
    }

    @Override
    public void deployClient(HostInventory h, List<String> protocols, Completion comp) {
        boolean deployVhostTarget = protocols != null && protocols.contains(VolumeProtocol.Vhost.toString());
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("deploy-zbs-client-on-host-%s", h.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "deploy-client";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        KVMHostVO host = Q.New(KVMHostVO.class).eq(KVMHostVO_.uuid, h.getUuid()).find();
                        if (host == null) {
                            trigger.fail(operr(ORG_ZSTACK_STORAGE_ZBS_10010, "cannot found kvm host[uuid:%s], unable to deploy client", h.getUuid()));
                            return;
                        }

                        DeployClientCmd cmd = new DeployClientCmd();
                        cmd.setIp(h.getManagementIp());
                        cmd.setPort(host.getPort());
                        cmd.setUsername(host.getUsername());
                        cmd.setPassword(host.getPassword());
                        httpCall(DEPLOY_CLIENT_PATH, cmd, DeployClientRsp.class, new ReturnValueCompletion<DeployClientRsp>(trigger) {
                            @Override
                            public void success(DeployClientRsp returnValue) {
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
                    String __name__ = "update-host-client-dependency";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        UpdateHostDependencyCmd cmd = new UpdateHostDependencyCmd();
                        cmd.updatePackages = "libcbd";
                        cmd.zstackRepo = AnsibleGlobalProperty.ZSTACK_REPO;

                        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                        msg.setCommand(cmd);
                        msg.setHostUuid(h.getUuid());
                        msg.setPath(UPDATE_HOST_DEPENDENCY_PATH);
                        msg.setNoStatusCheck(true);
                        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                KVMHostAsyncHttpCallReply hreply = reply.castReply();
                                UpdateHostDependencyRsp rsp = hreply.toResponse(UpdateHostDependencyRsp.class);
                                if (!rsp.isSuccess()) {
                                    trigger.fail(operr(ORG_ZSTACK_STORAGE_ZBS_10011, rsp.getError()));
                                    return;
                                }

                                trigger.next();
                            }
                        });
                    }
                });

                if (deployVhostTarget) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "prepare-vhost-target-env";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            PrepareVhostTargetEnvCmd cmd = new PrepareVhostTargetEnvCmd();

                            KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                            msg.setCommand(cmd);
                            msg.setHostUuid(h.getUuid());
                            msg.setPath(PREPARE_VHOST_TARGET_ENV_PATH);
                            msg.setNoStatusCheck(true);
                            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
                            bus.send(msg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        logger.warn(String.format("failed to prepare vhost target env on host[%s]: %s",
                                                h.getUuid(), reply.getError().getDetails()));
                                    } else {
                                        AgentResponse rsp = reply.<KVMHostAsyncHttpCallReply>castReply().toResponse(AgentResponse.class);
                                        if (!rsp.isSuccess()) {
                                            logger.warn(String.format("failed to prepare vhost target env on host[%s]: %s",
                                                    h.getUuid(), rsp.getError()));
                                        }
                                    }
                                    trigger.next();
                                }
                            });
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "deploy-vhost-target";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            KVMHostVO host = getKvmHost(h);
                            if (host == null) {
                                logger.warn(String.format("cannot find kvm host[uuid:%s], skip vhost target deploy", h.getUuid()));
                                trigger.next();
                                return;
                            }

                            DeployVhostCmd cmd = new DeployVhostCmd();
                            fillVhostHostParams(cmd, h, host);

                            httpCall(DEPLOY_VHOST_PATH, cmd, AgentResponse.class, new ReturnValueCompletion<AgentResponse>(trigger) {
                                @Override
                                public void success(AgentResponse rsp) {
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    logger.warn(String.format("failed to deploy vhost target on host[%s]: %s",
                                            h.getUuid(), errorCode.getDetails()));
                                    trigger.next();
                                }
                            });
                        }
                    });
                }

                done(new FlowDoneHandler(comp) {
                    @Override
                    public void handle(Map data) {
                        comp.success();
                    }
                });

                error(new FlowErrorHandler(comp) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        comp.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public synchronized void activateHeartbeatVolume(HostInventory h, ReturnValueCompletion<HeartbeatVolumeTopology> completion) {
        if (config == null) {
            reloadDbInfo();
        }

        // TODO: split by physical pool not logical pool, handle logical pool deletion
        HeartbeatVolumeTopology topology = new HeartbeatVolumeTopology();
        new While<>(config.getPoolNames()).each((poolName, comp) -> {
            CreateVolumeCmd cmd = new CreateVolumeCmd();
            cmd.setLogicalPool(poolName);
            cmd.setVolume(ZbsConstants.ZBS_HEARTBEAT_VOLUME_NAME);
            cmd.setSize(ZbsConstants.ZBS_HEARTBEAT_VOLUME_SIZE_IN_GIGABYTE);
            cmd.setSkipIfExisting(true);

            httpCall(CREATE_VOLUME_PATH, cmd, CreateVolumeRsp.class, new ReturnValueCompletion<CreateVolumeRsp>(comp) {
                @Override
                public void success(CreateVolumeRsp returnValue) {
                    CbdHeartbeatVolumeTO to = new CbdHeartbeatVolumeTO();
                    String zbsPath = returnValue.installPath;
                    to.setInstallPath(ZbsHelper.convertZbsPathToCbdPath(zbsPath, ZbsStorageController.this::getPhysicalPoolName));
                    to.setHeartbeatRequiredSpace(SizeUnit.MEGABYTE.toByte(1));
                    to.setCoveringPaths(config.getPoolNames());
                    topology.putHeartbeatVolume(poolName, to);
                    comp.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    comp.addError(errorCode);
                    comp.allDone();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    completion.fail(errorCodeList.getCauses().get(0));
                    return;
                }

                completion.success(topology);
            }
        });
    }

    @Override
    public void deactivateHeartbeatVolume(HostInventory h, Completion comp) {
        comp.success();
    }

    @Override
    public HeartbeatVolumeTopology getHeartbeatVolumeActiveInfo(HostInventory h) {
        if (config == null) {
            reloadDbInfo();
        }

        HeartbeatVolumeTopology topology = new HeartbeatVolumeTopology();
        Map<String, HeartbeatVolumeTO> map = new HashMap<>();
        for (String poolName : config.getPoolNames()) {
            String zbsPath = buildHeartbeatVolumePath(poolName);

            String cbdPath = ZbsHelper.convertZbsPathToCbdPath(zbsPath, this::getPhysicalPoolName);
            CbdHeartbeatVolumeTO to = new CbdHeartbeatVolumeTO();
            to.setInstallPath(cbdPath);
            to.setHeartbeatRequiredSpace(SizeUnit.MEGABYTE.toByte(1));
            to.setCoveringPaths(config.getPoolNames());

            map.put(poolName, to);
        }

        topology.setHeartbeatVolumeByCoveringPaths(map);
        return topology;
    }

    @Override
    public String getIdentity() {
        return ZbsConstants.IDENTITY;
    }

    @Override
    public void connect(String cfg, String url, ReturnValueCompletion<org.zstack.header.storage.addon.primary.AddonInfo> completion) {
        AddonInfo newAddonInfo = new AddonInfo();
        Config current = JSONObjectUtil.toObject(cfg, Config.class);
        List<MdsInfo> mdsInfos = MdsInfo.valueOf(current.getMdsUrls());
        newAddonInfo.setMdsInfos(mdsInfos);
        final List<ZbsPrimaryStorageMdsBase> mdsList = CollectionUtils.transformToList(newAddonInfo.getMdsInfos(),
                ZbsPrimaryStorageMdsBase::new);

        class Connector {
            private final ErrorCodeList errorCodes = new ErrorCodeList();
            private final Iterator<ZbsPrimaryStorageMdsBase> it = mdsList.iterator();

            void connect(final FlowTrigger trigger) {
                if (!it.hasNext()) {
                    if (errorCodes.getCauses().size() == mdsList.size()) {
                        if (errorCodes.getCauses().isEmpty()) {
                            trigger.fail(operr(ORG_ZSTACK_STORAGE_ZBS_10012, "unable to connect to the ZBS primary storage[uuid:%s]," +
                                    " failed to connect all MDS", self.getUuid()));
                        } else {
                            trigger.fail(operr(ORG_ZSTACK_STORAGE_ZBS_10013, errorCodes, "unable to connect to the ZBS primary storage[uuid:%s]," +
                                            " failed to connect all MDS",
                                    self.getUuid()));
                        }
                    } else {
                        ExternalPrimaryStorageVO vo = dbf.reload(self);
                        if (vo == null) {
                            trigger.fail(operr(ORG_ZSTACK_STORAGE_ZBS_10014, "ZBS primary storage[uuid:%s] may have been deleted", self.getUuid()));
                        } else {
                            self = vo;
                            addonInfo = newAddonInfo;
                            trigger.next();
                        }
                    }
                    return;
                }

                final ZbsPrimaryStorageMdsBase base = it.next();
                base.connect(new Completion(trigger) {
                    @Override
                    public void success() {
                        connect(trigger);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        errorCodes.getCauses().add(errorCode);
                        connect(trigger);
                    }
                });
            }
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("connect-zbs-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "connect-mds";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new Connector().connect(trigger);
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "get-facts";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        httpCall(GET_FACTS_PATH, new GetFactsCmd(), GetFactsRsp.class, new ReturnValueCompletion<GetFactsRsp>(trigger) {
                            @Override
                            public void success(GetFactsRsp returnValue) {
                                ClusterInfo info = new ClusterInfo();
                                info.setUuid(returnValue.getUuid());
                                info.setVersion(returnValue.getVersion());
                                newAddonInfo.setClusterInfo(info);
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
                    String __name__ = "deploy-client";

                    List<PrimaryStorageClusterRefVO> refs = Q.New(PrimaryStorageClusterRefVO.class)
                            .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, self.getUuid())
                            .list();

                    @Override
                    public boolean skip(Map data) {
                        return refs.isEmpty();
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> clusterUuids = refs.stream()
                                .map(PrimaryStorageClusterRefVO::getClusterUuid)
                                .collect(Collectors.toList());

                        List<HostVO> hosts = Q.New(HostVO.class)
                                .in(HostAO_.clusterUuid, clusterUuids)
                                .list();

                        new While<>(hosts).each((h, comp) -> {
                            KVMHostVO host = Q.New(KVMHostVO.class).eq(KVMHostVO_.uuid, h.getUuid()).find();
                            if (host == null) {
                                comp.addError(operr(ORG_ZSTACK_STORAGE_ZBS_10015, "cannot found kvm host[uuid:%s], unable to deploy client", ((HostVO) h).getUuid()));
                                comp.allDone();
                                return;
                            }

                            DeployClientCmd cmd = new DeployClientCmd();
                            cmd.setIp(h.getManagementIp());
                            cmd.setPort(host.getPort());
                            cmd.setUsername(host.getUsername());
                            cmd.setPassword(host.getPassword());
                            httpCall(DEPLOY_CLIENT_PATH, cmd, DeployClientRsp.class, new ReturnValueCompletion<DeployClientRsp>(comp) {
                                @Override
                                public void success(DeployClientRsp returnValue) {
                                    comp.done();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    comp.addError(errorCode);
                                    comp.allDone();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (!errorCodeList.getCauses().isEmpty()) {
                                    trigger.fail(errorCodeList.getCauses().get(0));
                                    return;
                                }

                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        addonInfo = newAddonInfo;
                        completion.success(newAddonInfo);
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        syncMdsStatuses(newAddonInfo);
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void reconnectSingleMds(ZbsPrimaryStorageMdsBase mdsBase, Completion completion) {
        mdsBase.connect(new Completion(completion) {
            @Override
            public void success() {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void reconnectMdss(List<ZbsPrimaryStorageMdsBase> mdsBases, Completion completion) {
        new While<>(mdsBases).all((mdsBase, comp) -> {
            reconnectSingleMds(mdsBase, new Completion(comp) {
                @Override
                public void success() {
                    comp.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    comp.addError(errorCode);
                    comp.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    completion.fail(errorCodeList.getCauses().get(0));
                    return;
                }
                completion.success();
            }
        });
    }

    private void incrementMdsPingFailureCount(ZbsPrimaryStorageMdsBase mds) {
        // initialize the count if absent
        mdsConsecutivePingFailureCount.putIfAbsent(mds.getSelf(), 0);
        // increment the count only if it is below the threshold
        // avoid unbounded growth of the count
        if (mdsConsecutivePingFailureCount.get(mds.getSelf()) < MDS_PING_FAIL_CYCLE_THRESHOLD) {
            mdsConsecutivePingFailureCount.merge(mds.getSelf(), 1, Integer::sum);
        }
    }

    private void resetMdsPingFailureCount(ZbsPrimaryStorageMdsBase mds) {
        mdsConsecutivePingFailureCount.remove(mds.getSelf());
    }

    private List<ZbsPrimaryStorageMdsBase> getDisconnectedMds(List<ZbsPrimaryStorageMdsBase> mdsBases) {
        return mdsBases.stream()
                .filter(m -> MdsStatus.Disconnected.equals(m.getSelf().getStatus()))
                .collect(Collectors.toList());
    }

    private boolean allMdssDisconnected(List<ZbsPrimaryStorageMdsBase> mdsBases) {
        List<ZbsPrimaryStorageMdsBase> disconnectedMds = getDisconnectedMds(mdsBases);
        return disconnectedMds.size() == mdsBases.size();
    }

    private List<ZbsPrimaryStorageMdsBase> getMdssNeedToReconnect(List<ZbsPrimaryStorageMdsBase> mdsBases) {
        List<ZbsPrimaryStorageMdsBase> disconnectedMds = getDisconnectedMds(mdsBases);
        boolean allDisconnected = allMdssDisconnected(mdsBases);

        if (disconnectedMds.isEmpty()) {
            return Collections.emptyList();
        }

        // If all MDS are disconnected, try to reconnect all of them
        if (allDisconnected) {
            logger.info("All MDS are disconnected, ignore the ping failure count threshold and all of them need be reconnected");
            return disconnectedMds;
        }

        String disconnectedMdsInfo = disconnectedMds.stream()
                .map(m -> String.format("%s (Consecutive Ping Failure: %d)",
                        m.getSelf().getAddr(),
                        mdsConsecutivePingFailureCount.computeIfAbsent(m.getSelf(), k -> 0)))
                .collect(Collectors.joining(", "));

        logger.info(String.format("Some MDS are disconnected for ZBS primary storage[uuid:%s], disconnected MDS: [%s]",
                self.getUuid(), disconnectedMdsInfo));

        return disconnectedMds.stream()
                .filter(m -> mdsConsecutivePingFailureCount
                        .computeIfAbsent(m.getSelf(), k -> 0) >= MDS_PING_FAIL_CYCLE_THRESHOLD)
                .collect(Collectors.toList());

    }

    @Override
    public void ping(ReturnValueCompletion<PingResult> completion) {
        reloadDbInfo();

        if (addonInfo == null || addonInfo.getClusterInfo() == null) {
            completion.fail(operr(ORG_ZSTACK_STORAGE_ZBS_10016, String.format("addon info is null, primary storage[uuid:%s] is not ready, skip ping task", self.getUuid())));
            return;
        }

        final List<ZbsPrimaryStorageMdsBase> mds = CollectionUtils.transformToList(addonInfo.getMdsInfos(), ZbsPrimaryStorageMdsBase::new);
        new While<>(mds).each((m, comp) -> m.ping(addonInfo.getClusterInfo(), new Completion(comp) {
            @Override
            public void success() {
                m.getSelf().setStatus(MdsStatus.Connected);
                resetMdsPingFailureCount(m);
                comp.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                m.getSelf().setStatus(MdsStatus.Disconnected);
                incrementMdsPingFailureCount(m);
                comp.done();
            }
        })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                List<ZbsPrimaryStorageMdsBase> mdsNeedToReconnect = getMdssNeedToReconnect(mds);

                if (mdsNeedToReconnect.isEmpty()) {
                    completion.success(new PingResult(addonInfo));
                    return;
                }

                String mdsNeedToReconnectInfo = mdsNeedToReconnect.stream()
                        .map(m -> String.format("%s (Consecutive Ping Failure: %d)",
                                m.getSelf().getAddr(),
                                mdsConsecutivePingFailureCount.get(m.getSelf())))
                        .collect(Collectors.joining(", "));

                logger.warn(String.format("Some MDS need to reconnect for ZBS primary storage[uuid:%s], MDS : %s",
                        self.getUuid(), mdsNeedToReconnectInfo));

                reconnectMdss(mdsNeedToReconnect, new Completion(completion) {
                    @Override
                    public void success() {
                        completion.success(new PingResult(addonInfo));
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        List<ZbsPrimaryStorageMdsBase> stillDisconnectedMds = getDisconnectedMds(mds);
                        boolean allDisconnected = allMdssDisconnected(mds);

                        if (allDisconnected) {
                            completion.success(new PingResult(addonInfo,
                                    String.format("All MDS are still disconnected after reconnection for ZBS primary storage[uuid:%s]",
                                            self.getUuid())));
                            return;
                        }

                        String stillDisconnectedMdsInfo =  stillDisconnectedMds.stream()
                                .map(m -> String.format("%s (Consecutive Ping Failure: %d)",
                                        m.getSelf().getAddr(),
                                        mdsConsecutivePingFailureCount.get(m.getSelf())))
                                .collect(Collectors.joining(", "));

                        logger.warn(String.format("Some MDS are still disconnected after reconnection for ZBS primary storage[uuid:%s], disconnected MDS : %s, error: %s",
                                self.getUuid(), stillDisconnectedMdsInfo, errorCode));

                        completion.success(new PingResult(addonInfo));
                    }
                });

            }
        });
    }

    @Override
    public void getCapacity(List<String> requiredUrls, ReturnValueCompletion<StorageCapacity> comp) {
        List<String> poolNames = requiredUrls.stream().map(ZbsHelper::getPoolFromVolumePath).collect(Collectors.toList());
        getPoolCapacities(poolNames, comp);
    }

    @Override
    public void reportCapacity(ReturnValueCompletion<StorageCapacity> comp) {
        getPoolCapacities(config.getPoolNames(), comp);
    }

    private void getPoolCapacities(List<String> poolNames, ReturnValueCompletion<StorageCapacity> comp) {
        GetCapacityCmd cmd = new GetCapacityCmd();
        cmd.logicalPoolNames = poolNames;

        httpCall(GET_CAPACITY_PATH, cmd, GetCapacityRsp.class, new ReturnValueCompletion<GetCapacityRsp>(comp) {
            @Override
            public void success(GetCapacityRsp returnValue) {
                reloadDbInfo();
                addonInfo.setLogicalPoolInfos(returnValue.getLogicalPoolInfos());
                SQL.New(ExternalPrimaryStorageVO.class).eq(ExternalPrimaryStorageVO_.uuid, self.getUuid())
                        .set(ExternalPrimaryStorageVO_.addonInfo, JSONObjectUtil.toJsonString(addonInfo))
                        .update();

                List<LogicalPoolInfo> logicalPoolInfos = getSelfPools();
                long total = logicalPoolInfos.stream().mapToLong(LogicalPoolInfo::getCapacity).sum();
                long used = logicalPoolInfos.stream().mapToLong(LogicalPoolInfo::getUsedSize).sum();
                long avail = total != 0 ? total - used : 0;

                StorageCapacity cap = new StorageCapacity();
                cap.setTotalCapacity(total);
                cap.setAvailableCapacity(avail);
                cap.setHealthy(StorageHealthy.Ok);
                for (LogicalPoolInfo pool : logicalPoolInfos) {
                    cap.putCapacity(buildPoolPath(pool.getLogicalPoolName()), pool.getCapacity() - pool.getUsedSize(), pool.getCapacity());
                }
                comp.success(cap);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.fail(errorCode);
            }
        });
    }

    @Override
    public void reportHealthy(ReturnValueCompletion<StorageHealthy> comp) {
        // TODO: more accurate healthy report
        getPoolCapacities(config.getPoolNames(), new ReturnValueCompletion<StorageCapacity>(comp) {
            @Override
            public void success(StorageCapacity returnValue) {
                comp.success(StorageHealthy.Ok);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.success(StorageHealthy.Unknown);
            }
        });
    }

    @Override
    public void reportNodeHealthy(HostInventory host, ReturnValueCompletion<NodeHealthy> comp) {
        CheckHostStorageConnectionCmd cmd = new CheckHostStorageConnectionCmd();
        cmd.setHostUuid(host.getUuid());
        String zbsHbPath = buildHeartbeatVolumePath(config.getLogicalPoolName());
        cmd.setPath(ZbsHelper.convertZbsPathToCbdPath(zbsHbPath, this::getPhysicalPoolName));

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(host.getUuid());
        msg.setPath(CHECK_HOST_STORAGE_CONNECTION_PATH);
        msg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(comp) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    comp.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply hreply = reply.castReply();
                CheckHostStorageConnectionRsp rsp = hreply.toResponse(CheckHostStorageConnectionRsp.class);
                NodeHealthy healthy = new NodeHealthy();
                healthy.setHealthy(VolumeProtocol.CBD, rsp.isSuccess() ? StorageHealthy.Ok : StorageHealthy.Failed);

                if (!supportsVhost()) {
                    comp.success(healthy);
                    return;
                }
                checkVhostTargetHealthy(host, healthy, comp);
            }
        });
    }

    private boolean supportsVhost() {
        return Q.New(PrimaryStorageOutputProtocolRefVO.class)
                .eq(PrimaryStorageOutputProtocolRefVO_.primaryStorageUuid, self.getUuid())
                .eq(PrimaryStorageOutputProtocolRefVO_.outputProtocol, VolumeProtocol.Vhost.toString())
                .isExists();
    }

    private void checkVhostTargetHealthy(HostInventory host, NodeHealthy healthy, ReturnValueCompletion<NodeHealthy> comp) {
        VhostTargetHealthCmd cmd = new VhostTargetHealthCmd();
        cmd.containerName = ZbsConstants.VHOST_TARGET_CONTAINER_PREFIX + host.getManagementIp();
        cmd.controlSock = ZbsConstants.VHOST_SOCKET_DIR + "/" + ZbsConstants.VHOST_ADMIN_SOCK_NAME;

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(host.getUuid());
        msg.setPath(VHOST_TARGET_HEALTH_PATH);
        msg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(comp) {
            @Override
            public void run(MessageReply reply) {
                boolean targetHealthy = false;
                if (reply.isSuccess()) {
                    VhostTargetHealthRsp rsp = reply.<KVMHostAsyncHttpCallReply>castReply()
                            .toResponse(VhostTargetHealthRsp.class);
                    targetHealthy = rsp.isSuccess() && rsp.targetRunning;
                }
                healthy.setHealthy(VolumeProtocol.Vhost, targetHealthy ? StorageHealthy.Ok : StorageHealthy.Failed);
                comp.success(healthy);
            }
        });
    }

    @Override
    public StorageCapabilities reportCapabilities() {
        return capabilities;
    }

    @Override
    public String allocateSpace(AllocateSpaceSpec aspec) {
        if (config == null || addonInfo == null) {
            reloadDbInfo();
        }

        Predicate<LogicalPoolInfo> p = aspec.getRequiredUrl() == null ? null : it -> {
            String pool = getPoolFromVolumePath(aspec.getRequiredUrl());
            return StringUtils.equals(pool, it.getLogicalPoolName());
        };

        LogicalPoolInfo logicalPoolInfo = allocateFreePool(aspec.getSize(), p);
        if (logicalPoolInfo == null) {
            throw new OperationFailureException(operr(ORG_ZSTACK_STORAGE_ZBS_10017, "no available logical pool with enough space[%d] and required url: %s",
                    aspec.getSize(), aspec.getRequiredUrl()));
        }

        return buildPoolPath(logicalPoolInfo.getLogicalPoolName());
    }

    private LogicalPoolInfo allocateFreePool(long size, Predicate<LogicalPoolInfo> filter) {
        List<LogicalPoolInfo> logicalPoolInfos = getSelfPools();
        Stream<LogicalPoolInfo> s = logicalPoolInfos.stream().filter(it -> it.getCapacity() - it.getUsedSize() > size);
        if (filter != null) {
            s = s.filter(filter);
        }

        return s.max(Comparator.comparingLong(it -> it.getCapacity() - it.getUsedSize()))
                .orElse(null);
    }

    private List<LogicalPoolInfo> getSelfPools() {
        return addonInfo.getLogicalPoolInfos().stream().filter(pool ->
                config.getPoolNames().contains(pool.getLogicalPoolName())).collect(Collectors.toList());
    }

    @Override
    public void createVolume(CreateVolumeSpec v, ReturnValueCompletion<VolumeStats> comp) {
        reloadDbInfo();

        CreateVolumeCmd cmd = new CreateVolumeCmd();
        cmd.setVolume(v.getName());
        cmd.setUnit(getSizeUnit(addonInfo.getClusterInfo().getVersion()));
        cmd.setSize(alignSizeTo(v.getSize(), cmd.getUnit()));
        cmd.setSkipIfExisting(true);
        String poolName = v.getAllocatedUrl() != null ? getPoolFromVolumePath(v.getAllocatedUrl()) :
                allocateFreePool(cmd.getSize(), null).getLogicalPoolName();
        cmd.setLogicalPool(poolName);

        httpCall(CREATE_VOLUME_PATH, cmd, CreateVolumeRsp.class, new ReturnValueCompletion<CreateVolumeRsp>(comp) {
            @Override
            public void success(CreateVolumeRsp returnValue) {
                VolumeStats stats = new VolumeStats();
                stats.setInstallPath(returnValue.installPath);
                stats.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                stats.setSize(returnValue.getSize());
                stats.setActualSize(returnValue.getActualSize());
                comp.success(stats);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.fail(errorCode);
            }
        });
    }

    @Override
    public void deleteVolume(String installPath, Completion comp) {
        doDeleteVolume(installPath, true, comp);
    }

    @Override
    public void deleteVolumeAndSnapshot(String installPath, Completion comp) {
        doDeleteVolume(installPath, true, comp);
    }

    @Override
    public void trashVolume(String installPath, Completion comp) {
        doDeleteVolume(installPath, false, comp);
    }

    @Override
    public void cloneVolume(String srcInstallPath, CreateVolumeSpec dst, ReturnValueCompletion<VolumeStats> comp) {
        VolumeStats stats = new VolumeStats();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("clone-volume-%s", dst.getName()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "clone-volume";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CloneVolumeCmd cmd = new CloneVolumeCmd();
                        cmd.setPath(srcInstallPath);
                        cmd.setDstVolume(dst.getName());

                        httpCall(CLONE_VOLUME_PATH, cmd, CloneVolumeRsp.class, new ReturnValueCompletion<CloneVolumeRsp>(trigger) {
                            @Override
                            public void success(CloneVolumeRsp returnValue) {
                                stats.setInstallPath(returnValue.installPath);
                                stats.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                                stats.setSize(returnValue.getSize());
                                stats.setActualSize(returnValue.getActualSize());
                                stats.setParentUri(srcInstallPath);
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
                    String __name__ = "resize-volume";

                    @Override
                    public boolean skip(Map data) {
                        return stats.getSize() >= dst.getSize();
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ExpandVolumeCmd cmd = new ExpandVolumeCmd();
                        cmd.setPath(convertZbsPathToCbdPath(stats.getInstallPath(), ZbsStorageController.this::getPhysicalPoolName));
                        cmd.setUnit(getSizeUnit(addonInfo.getClusterInfo().getVersion()));
                        cmd.setSize(alignSizeTo(dst.getSize(), cmd.getUnit()));

                        httpCall(EXPAND_VOLUME_PATH, cmd, ExpandVolumeRsp.class, new ReturnValueCompletion<ExpandVolumeRsp>(trigger) {
                            @Override
                            public void success(ExpandVolumeRsp returnValue) {
                                stats.setSize(returnValue.getSize());
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(comp) {
                    @Override
                    public void handle(Map data) {
                        comp.success(stats);
                    }
                });

                error(new FlowErrorHandler(comp) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        comp.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public void copyVolume(String srcInstallPath, CreateVolumeSpec dst, ReturnValueCompletion<VolumeStats> comp) {
        CopyCmd cmd = new CopyCmd();
        cmd.setPath(srcInstallPath);
        cmd.setDstVolume(dst.getName());
        if (dst.getAllocatedUrl() != null) {
            cmd.setDstPool(getPoolFromVolumePath(dst.getAllocatedUrl()));
        }

        httpCall(COPY_PATH, cmd, CopyRsp.class, new ReturnValueCompletion<CopyRsp>(comp) {
            @Override
            public void success(CopyRsp returnValue) {
                VolumeStats stats = new VolumeStats();
                stats.setInstallPath(returnValue.getInstallPath());
                stats.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                stats.setSize(returnValue.getSize());
                stats.setActualSize(returnValue.getActualSize());
                comp.success(stats);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.fail(errorCode);
            }
        });
    }

    @Override
    public void flattenVolume(String installPath, ReturnValueCompletion<VolumeStats> comp) {
        FlattenVolumeCmd cmd = new FlattenVolumeCmd();
        cmd.setPath(installPath);

        httpCall(FLATTEN_VOLUME_PATH, cmd, FlattenVolumeRsp.class, new ReturnValueCompletion<FlattenVolumeRsp>(comp) {
            @Override
            public void success(FlattenVolumeRsp returnValue) {
                VolumeStats stats = new VolumeStats();
                stats.setInstallPath(installPath);
                stats.setSize(returnValue.getSize());
                stats.setActualSize(returnValue.getActualSize());
                stats.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                stats.setParentUri(ZbsHelper.normalizeToZbsPath(returnValue.getParentUri()));
                comp.success(stats);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.fail(errorCode);
            }
        });
    }

    @Override
    public void stats(String installPath, ReturnValueCompletion<VolumeStats> comp) {
        QueryVolumeCmd cmd = new QueryVolumeCmd();
        cmd.setPath(installPath);

        httpCall(QUERY_VOLUME_PATH, cmd, QueryVolumeRsp.class, new ReturnValueCompletion<QueryVolumeRsp>(comp) {
            @Override
            public void success(QueryVolumeRsp returnValue) {
                VolumeStats stats = new VolumeStats();
                stats.setInstallPath(installPath);
                stats.setSize(returnValue.getSize());
                stats.setActualSize(returnValue.getActualSize());
                stats.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                stats.setParentUri(ZbsHelper.normalizeToZbsPath(returnValue.getParentUri()));
                comp.success(stats);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.fail(errorCode);
            }
        });
    }

    @Override
    public void batchStats(Collection<String> installPaths, ReturnValueCompletion<List<VolumeStats>> comp) {
        BatchQueryVolumeCmd cmd = new BatchQueryVolumeCmd();

        cmd.setInstallPaths(installPaths.stream().map(it -> convertZbsPathToCbdPath(it, this::getPhysicalPoolName))
                .collect(Collectors.toList()));

        httpCall(BATCH_QUERY_VOLUME_PATH, cmd, BatchQueryVolumeRsp.class, new ReturnValueCompletion<BatchQueryVolumeRsp>(comp) {
            @Override
            public void success(BatchQueryVolumeRsp returnValue) {
                List<VolumeStats> stats = returnValue.getVolumes().entrySet().stream().map(v -> {
                    VolumeStats s = new VolumeStats();
                    s.setInstallPath(ZbsHelper.normalizeToZbsPath(v.getKey()));
                    s.setSize(v.getValue().get("length"));
                    s.setActualSize(v.getValue().get("usedSize"));
                    s.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                    return s;
                }).collect(Collectors.toList());
                comp.success(stats);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.fail(errorCode);
            }
        });
    }

    @Override
    public void expandVolume(String installPath, long size, ReturnValueCompletion<VolumeStats> comp) {
        ExpandVolumeCmd cmd = new ExpandVolumeCmd();
        cmd.setPath(convertZbsPathToCbdPath(installPath, ZbsStorageController.this::getPhysicalPoolName));
        cmd.setUnit(getSizeUnit(addonInfo.getClusterInfo().getVersion()));
        cmd.setSize(alignSizeTo(size, cmd.getUnit()));

        httpCall(EXPAND_VOLUME_PATH, cmd, ExpandVolumeRsp.class, new ReturnValueCompletion<ExpandVolumeRsp>(comp) {
            @Override
            public void success(ExpandVolumeRsp returnValue) {
                VolumeStats stats = new VolumeStats();
                stats.setInstallPath(installPath);
                stats.setSize(returnValue.getSize());
                comp.success(stats);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.fail(errorCode);
            }
        });
    }

    @Override
    public void setVolumeQos(BaseVolumeInfo v, Completion comp) {
        comp.success();
    }

    @Override
    public void deleteVolumeQos(BaseVolumeInfo v, Completion comp) {
        comp.success();
    }

    @Override
    public void export(ExportSpec espec, VolumeProtocol protocol, ReturnValueCompletion<RemoteTarget> comp) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            NbdRemoteTarget target = new NbdRemoteTarget();
            target.setIp("127.0.0.1");
            target.setPort(6666);
            comp.success(target);
            return;
        }

        if (protocol != VolumeProtocol.NBD) {
            comp.fail(operr(ORG_ZSTACK_STORAGE_ZBS_10018, "unsupported protocol %s", protocol.name()));
            return;
        }

        CbdToNbdCmd cmd = new CbdToNbdCmd();
        cmd.setPath(espec.getInstallPath());
        cmd.setPortRange(HostGlobalConfig.NBD_PORT_RANGE.value(String.class));

        httpCall(CBD_TO_NBD_PATH, cmd, CbdToNbdRsp.class, new ReturnValueCompletion<CbdToNbdRsp>(comp) {
            @Override
            public void success(CbdToNbdRsp returnValue) {
                NbdRemoteTarget target = new NbdRemoteTarget();
                target.setIp(returnValue.getIp());
                target.setPort(returnValue.getPort());
                comp.success(target);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.fail(errorCode);
            }
        });
    }

    @Override
    public void unexport(ExportSpec espec, RemoteTarget remoteTarget, VolumeProtocol protocol, Completion comp) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            comp.success();
            return;
        }

        if (protocol != VolumeProtocol.NBD) {
            comp.fail(operr(ORG_ZSTACK_STORAGE_ZBS_10019, "unsupported protocol %s", protocol.name()));
            return;
        }

        if (remoteTarget == null || remoteTarget.getResourceURI() == null) {
            logger.debug("remote target or the URI does not exist");
            comp.success();
            return;
        }

        try {
            URI uri = new URI(remoteTarget.getResourceURI());

            CleanNbdCmd cmd = new CleanNbdCmd();
            cmd.setNbdAddr(uri.getHost());
            cmd.setPort(uri.getPort());
            new HttpCaller<>(CLEAN_NBD_PATH, cmd, AgentResponse.class, new ReturnValueCompletion<AgentResponse>(comp) {
                @Override
                public void success(AgentResponse returnValue) {
                    comp.success();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    comp.fail(errorCode);
                }
            }).setTargetMds(uri.getHost()).call();
        } catch (URISyntaxException e) {
            comp.fail(operr(ORG_ZSTACK_STORAGE_ZBS_10020, "invalid URI syntax: %s", e.getMessage()));
        }
    }

    @Override
    public void createSnapshot(CreateVolumeSnapshotSpec spec, ReturnValueCompletion<VolumeSnapshotStats> comp) {
        CreateSnapshotCmd cmd = new CreateSnapshotCmd();
        cmd.setPath(spec.getVolumeInstallPath());
        cmd.setSnapshot(spec.getName());
        cmd.setSkipOnExisting(true);

        httpCall(CREATE_SNAPSHOT_PATH, cmd, CreateSnapshotRsp.class, new ReturnValueCompletion<CreateSnapshotRsp>(comp) {
            @Override
            public void success(CreateSnapshotRsp returnValue) {
                VolumeSnapshotStats stats = new VolumeSnapshotStats();
                stats.setInstallPath(returnValue.getInstallPath());
                stats.setActualSize(returnValue.getActualSize());
                comp.success(stats);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.fail(errorCode);
            }
        });
    }

    @Override
    public void deleteSnapshot(String installPath, Completion comp) {
        DeleteSnapshotCmd cmd = new DeleteSnapshotCmd();
        cmd.setPath(installPath);

        httpCall(DELETE_SNAPSHOT_PATH, cmd, DeleteSnapshotRsp.class, new ReturnValueCompletion<DeleteSnapshotRsp>(comp) {
            @Override
            public void success(DeleteSnapshotRsp returnValue) {
                comp.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.fail(errorCode);
            }
        });
    }

    @Override
    public void expungeSnapshot(String installPath, Completion comp) {
        comp.success();
    }

    @Override
    public void revertVolumeSnapshot(String snapshotInstallPath, ReturnValueCompletion<VolumeStats> comp) {
        RollbackSnapshotCmd cmd = new RollbackSnapshotCmd();
        cmd.setPath(snapshotInstallPath);

        httpCall(ROLLBACK_SNAPSHOT_PATH, cmd, RollbackSnapshotRsp.class, new ReturnValueCompletion<RollbackSnapshotRsp>(comp) {
            @Override
            public void success(RollbackSnapshotRsp returnValue) {
                VolumeStats stats = new VolumeStats();
                stats.setInstallPath(returnValue.getInstallPath());
                stats.setSize(returnValue.getSize());
                stats.setActualSize(returnValue.getActualSize());
                stats.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                comp.success(stats);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.fail(errorCode);
            }
        });
    }

    @Override
    public String validateConfig(String config) throws ApiMessageInterceptionException {
        Config old = JSONObjectUtil.toObject(self.getConfig(), Config.class);
        Config current = JSONObjectUtil.toObject(config, Config.class);
        if (current.getLogicalPoolName() == null && !CollectionUtils.isEmpty(current.getPools())) {
            if (current.getPoolNames().contains(old.getLogicalPoolName())) {
                current.setLogicalPoolName(old.getLogicalPoolName());
            } else {
                current.setLogicalPoolName(current.getPoolNames().get(0));
            }
        }

        if (current.getLogicalPoolName() != null && CollectionUtils.isEmpty(current.getPools())) {
            current.setPools(Arrays.asList(new Config.Pool(current.getLogicalPoolName(), null)));
        }

        if (current.getLogicalPoolName().contains("/")) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_STORAGE_ZBS_10021, "invalid logical pool name[%s]", current.getLogicalPoolName()));
        }

        if (!current.getPoolNames().contains(current.getLogicalPoolName())) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_STORAGE_ZBS_10022, "invalid pool name[%s]", current.getLogicalPoolName()));
        }

        if (CollectionUtils.isEmpty(current.getPoolNames())) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_STORAGE_ZBS_10023, "ensure at least one pool is configured"));
        }

        if (CollectionUtils.isEmpty(current.getMdsUrls())) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_STORAGE_ZBS_10024, "ensure at least one MDS is configured"));
        }

        List<MdsInfo> newMdsInfos = MdsInfo.valueOf(current.getMdsUrls());
        List<MdsInfo> duplicateMdsInfos = newMdsInfos.stream().collect(Collectors.groupingBy(MdsInfo::getAddr))
                .values().stream().filter(addr -> addr.size() > 1).flatMap(List::stream).collect(Collectors.toList());
        if (!duplicateMdsInfos.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_STORAGE_ZBS_10025, "do not allow to add duplicate MDS[%s]",
                    duplicateMdsInfos.stream().map(MdsInfo::getAddr).distinct().collect(Collectors.joining(", "))
            ));
        }

        List<MdsInfo> oldMdsInfos = MdsInfo.valueOf(old.getMdsUrls());
        List<MdsInfo> changedMdsInfos = newMdsInfos.stream().filter(n -> oldMdsInfos.stream().noneMatch(o -> o.equals(n))).collect(Collectors.toList());
        if (!changedMdsInfos.isEmpty() && !CoreGlobalProperty.UNIT_TEST_ON) {
            List<ZbsPrimaryStorageMdsBase> mdsList = CollectionUtils.transformToList(changedMdsInfos, ZbsPrimaryStorageMdsBase::new);
            for (ZbsPrimaryStorageMdsBase base : mdsList) {
                base.checkSshAndTools();
                base.checkStorageHealth();
            }
        }
        return JSONObjectUtil.toJsonString(current);
    }

    @Override
    public void setTrashExpireTime(int timeInSeconds, Completion completion) {
        completion.success();
    }

    @Override
    public void onFirstAdditionConfigure(Completion completion) {
        ResourceConfig rc = rcf.getResourceConfig(VolumeGlobalConfig.VOLUME_PHYSICAL_BLOCK_SIZE.getIdentity());
        rc.updateValue(self.getUuid(), ZbsConstants.VOLUME_PHYSICAL_BLOCK_SIZE);
        completion.success();
    }

    @Override
    public long alignSize(long size) {
        String unit = getSizeUnit(addonInfo.getClusterInfo().getVersion());
        return convertSizeToByte(alignSizeTo(size, unit), unit);
    }

    public void doDeleteVolume(String installPath, Boolean force, Completion comp) {
        DeleteVolumeCmd cmd = new DeleteVolumeCmd();
        cmd.setPath(installPath);
        cmd.setForce(force);

        httpCall(DELETE_VOLUME_PATH, cmd, DeleteVolumeRsp.class, new ReturnValueCompletion<DeleteVolumeRsp>(comp) {
            @Override
            public void success(DeleteVolumeRsp returnValue) {
                comp.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.fail(errorCode);
            }
        });
    }

    @Override
    public void syncAddonInfo(String addonInfo) {
        this.addonInfo = StringUtils.isEmpty(addonInfo) ? new AddonInfo() : JSONObjectUtil.toObject(addonInfo, AddonInfo.class);
    }

    @Override
    public void syncConfig(String config) {
        this.config = StringUtils.isEmpty(config) ? new Config() : JSONObjectUtil.toObject(config, Config.class);
    }

    private void syncMdsStatuses(AddonInfo newAddonInfo) {
        if (addonInfo == null || newAddonInfo == null) {
            return;
        }

        for (MdsInfo newMds : newAddonInfo.getMdsInfos()) {
            for (MdsInfo existMds : addonInfo.getMdsInfos()) {
                if (existMds.getAddr().equals(newMds.getAddr())) {
                    existMds.setStatus(newMds.getStatus());
                }
            }
        }

        SQL.New(ExternalPrimaryStorageVO.class).eq(ExternalPrimaryStorageVO_.uuid, self.getUuid())
                .set(ExternalPrimaryStorageVO_.addonInfo, JSONObjectUtil.toJsonString(addonInfo))
                .update();
    }

    @Deprecated
    private void reloadDbInfo() {
        self = dbf.reload(self);
        addonInfo = StringUtils.isEmpty(self.getAddonInfo()) ? new AddonInfo() : JSONObjectUtil.toObject(self.getAddonInfo(), AddonInfo.class);
        config = StringUtils.isEmpty(self.getConfig()) ? new Config() : JSONObjectUtil.toObject(self.getConfig(), Config.class);
        physicalPoolByLogicalPool = addonInfo.getLogicalPoolInfos().stream()
                .collect(Collectors.toMap(LogicalPoolInfo::getLogicalPoolName, LogicalPoolInfo::getPhysicalPoolName));
    }

    protected String getPhysicalPoolName(String logicalPoolName) {
        if (physicalPoolByLogicalPool.containsKey(logicalPoolName)) {
            return physicalPoolByLogicalPool.get(logicalPoolName);
        } else {
            reloadDbInfo();
            String physicalPool = physicalPoolByLogicalPool.get(logicalPoolName);
            if (physicalPool == null) {
                throw new OperationFailureException(operr(ORG_ZSTACK_STORAGE_ZBS_10026, "cannot find physical pool for logical pool[%s]", logicalPoolName));
            }
            return physicalPool;
        }
    }

    protected <T extends AgentResponse> T syncHttpCall(final String path, final AgentCommand cmd, final Class<T> retClass) {
        return httpCall(path, cmd, retClass, null, 0, true);
    }

    protected <T extends AgentResponse> T httpCall(final String path, final AgentCommand cmd, final Class<T> retClass, TimeUnit unit, long timeout, boolean sync) {
        return new HttpCaller<>(path, cmd, retClass, null, unit, timeout, sync).syncCall();
    }

    protected <T extends AgentResponse> void httpCall(final String path, final AgentCommand cmd, final Class<T> retClass, final ReturnValueCompletion<T> callback) {
        httpCall(path, cmd, retClass, callback, null, 0, false);
    }

    protected <T extends AgentResponse> void httpCall(final String path, final AgentCommand cmd, final Class<T> retClass, final ReturnValueCompletion<T> callback, TimeUnit unit, long timeout, boolean sync) {
        new HttpCaller<>(path, cmd, retClass, callback, unit, timeout, sync).call();
    }

    public class HttpCaller<T extends AgentResponse> {
        private Iterator<ZbsPrimaryStorageMdsBase> it;
        private final List<MdsInfo> mdsInfos;
        private final ErrorCodeList errorCodes = new ErrorCodeList();

        private final String path;
        private final AgentCommand cmd;
        private final Class<T> retClass;
        private final ReturnValueCompletion<T> callback;
        private final TimeUnit unit;
        private final long timeout;
        private final boolean sync;

        private boolean tryNext = false;

        HttpCaller<T> setTryNext(boolean tryNext) {
            this.tryNext = tryNext;
            return this;
        }

        public HttpCaller(String path, AgentCommand cmd, Class<T> retClass, ReturnValueCompletion<T> callback) {
            this(path, cmd, retClass, callback, null, 0, false);
        }

        public HttpCaller(String path, AgentCommand cmd, Class<T> retClass, ReturnValueCompletion<T> callback, TimeUnit unit, long timeout, boolean sync) {
            this.path = path;
            this.cmd = cmd;
            this.retClass = retClass;
            this.callback = callback;
            this.unit = unit;
            this.timeout = timeout;
            this.mdsInfos = prepareMds();
            this.sync = sync;
        }

        public void call() {
            prepareMdsIterator();
            prepareCmd();
            doCall();
        }

        public T syncCall() {
            prepareMdsIterator();
            prepareCmd();
            return doSyncCall();
        }

        HttpCaller<T> setTargetMds(String mdsAddr) {
            logger.debug(String.format("target MDS[%s]", mdsAddr));

            mdsInfos.removeIf(it -> !it.getAddr().equals(mdsAddr));
            if (mdsInfos.isEmpty()) {
                throw new OperationFailureException(operr(
                        ORG_ZSTACK_STORAGE_ZBS_10027, "not found MDS[%s] of zbs primary storage[uuid:%s] node", mdsAddr, self.getUuid())
                );
            }

            return this;
        }

        private void prepareCmd() {
            if (cmd instanceof VolumeCommand) {
                String cbdPath = convertZbsPathToCbdPath(((VolumeCommand) cmd).getPath(), ZbsStorageController.this::getPhysicalPoolName);
                ((VolumeCommand) cmd).setPath(cbdPath);
            }
            cmd.setUuid(self.getUuid());
        }

        private List<MdsInfo> prepareMds() {
            final List<MdsInfo> mds = new ArrayList<>(addonInfo.getMdsInfos());

            Collections.shuffle(mds);

            mds.removeIf(it -> it.getStatus() != MdsStatus.Connected);
            if (mds.isEmpty()) {
                throw new OperationFailureException(operr(
                        ORG_ZSTACK_STORAGE_ZBS_10028, "all MDS of ZBS primary storage[uuid:%s] are not in Connected state", self.getUuid())
                );
            }

            return mds;
        }

        private void prepareMdsIterator() {
            it = mdsInfos.stream().map(ZbsPrimaryStorageMdsBase::new).collect(Collectors.toList()).iterator();
        }

        private T doSyncCall() {
            if (!it.hasNext()) {
                throw new OperationFailureException(operr(ORG_ZSTACK_STORAGE_ZBS_10029, errorCodes, "all MDS cannot execute http call[%s]", path));
            }

            ZbsPrimaryStorageMdsBase base = it.next();
            cmd.setAddr(base.getSelf().getAddr());

            T ret = base.syncCall(path, cmd, retClass, unit, timeout);
            if (!ret.isSuccess()) {
                logger.warn(String.format("failed to execute http call[%s] on MDS[%s], error is: %s",
                        path, base.getSelf().getAddr(), JSONObjectUtil.toJsonString(ret.getError())));
                errorCodes.getCauses().add(operr(ORG_ZSTACK_STORAGE_ZBS_10030, ret.getError()));
                if (tryNext) {
                    return doSyncCall();
                } else {
                    throw new OperationFailureException(operr(ORG_ZSTACK_STORAGE_ZBS_10031, errorCodes, "all MDS cannot execute http call[%s]", path));
                }
            }

            return ret;
        }

        private void doCall() {
            if (!it.hasNext()) {
                callback.fail(operr(ORG_ZSTACK_STORAGE_ZBS_10032, errorCodes, "all MDS cannot execute http call[%s]", path));
                return;
            }

            ZbsPrimaryStorageMdsBase base = it.next();
            cmd.setAddr(base.getSelf().getAddr());

            ReturnValueCompletion<T> completion = new ReturnValueCompletion<T>(callback) {
                @Override
                public void success(T ret) {
                    if (ret instanceof VolumeResponse) {
                        String path = ((VolumeResponse) ret).installPath;
                        if (path != null && path.startsWith("cbd:")) {
                            ((VolumeResponse) ret).installPath = ZbsHelper.convertCbdPathToZbsPath(path);
                        }
                    }

                    callback.success(ret);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    if (!errorCode.isError(SysErrors.OPERATION_ERROR) && !errorCode.isError(SysErrors.TIMEOUT)) {
                        logger.warn(String.format("failed to execute http call[%s] on MDS[%s], error is: %s",
                                path, base.getSelf().getAddr(), JSONObjectUtil.toJsonString(errorCode)));
                        errorCodes.getCauses().add(errorCode);
                        doCall();
                        return;
                    }

                    if (tryNext) {
                        doCall();
                    } else {
                        callback.fail(errorCode);
                    }
                }
            };

            if (unit == null) {
                base.httpCall(path, cmd, retClass, completion);
            } else {
                base.httpCall(path, cmd, retClass, completion, unit, timeout);
            }
        }
    }

    public static class ExpandVolumeRsp extends AgentResponse {
        private long size;

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    public static class CopyRsp extends VolumeResponse {
    }

    public static class RollbackSnapshotRsp extends VolumeResponse {
    }

    public static class DeleteSnapshotRsp extends AgentResponse {

    }

    public static class QueryVolumeRsp extends AgentResponse {
        private long size;
        private long actualSize;
        private String parentUri;

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public long getActualSize() {
            return actualSize;
        }

        public void setActualSize(long actualSize) {
            this.actualSize = actualSize;
        }

        public String getParentUri() {
            return parentUri;
        }

        public void setParentUri(String parentUri) {
            this.parentUri = parentUri;
        }
    }

    public static class FlattenVolumeRsp extends QueryVolumeRsp {

    }

    public static class BatchQueryVolumeRsp extends AgentResponse {
        private Map<String, Map<String, Long>> volumes;

        public Map<String, Map<String, Long>> getVolumes() {
            return volumes;
        }

        public void setVolumes(Map<String, Map<String, Long>> volumes) {
            this.volumes = volumes;
        }
    }

    public static class CbdToNbdRsp extends AgentResponse {
        private String ip;
        private int port;

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    public static class CloneVolumeRsp extends VolumeResponse {
    }

    public static class CreateSnapshotRsp extends VolumeResponse {
    }

    public static class DeleteVolumeRsp extends AgentResponse {
    }

    public static class CreateVolumeRsp extends VolumeResponse {
    }

    public static class GetCapacityRsp extends AgentResponse {
        private List<LogicalPoolInfo> logicalPoolInfos;

        public List<LogicalPoolInfo> getLogicalPoolInfos() {
            return logicalPoolInfos;
        }

        public void setLogicalPoolInfos(List<LogicalPoolInfo> logicalPoolInfos) {
            this.logicalPoolInfos = logicalPoolInfos;
        }
    }

    public static class DeployClientRsp extends AgentResponse {

    }

    public static class ExpandVolumeCmd extends SizeCmd {
        private String path;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class CopyCmd extends VolumeCommand implements HasThreadContext {
        private String dstVolume;
        private String dstPool;

        public String getDstVolume() {
            return dstVolume;
        }

        public void setDstVolume(String dstVolume) {
            this.dstVolume = dstVolume;
        }

        public void setDstPool(String dstPool) {
            this.dstPool = dstPool;
        }

        public String getDstPool() {
            return dstPool;
        }
    }

    public static class RollbackSnapshotCmd extends VolumeCommand {
    }

    public static class DeleteSnapshotCmd extends VolumeCommand {
    }

    public static class QueryVolumeCmd extends VolumeCommand {
    }

    public static class FlattenVolumeCmd extends VolumeCommand {
    }

    public static class BatchQueryVolumeCmd extends AgentCommand {
        private Collection<String> installPaths;

        public Collection<String> getInstallPaths() {
            return installPaths;
        }

        public void setInstallPaths(Collection<String> installPaths) {
            this.installPaths = installPaths;
        }
    }

    public static class CleanNbdCmd extends AgentCommand {
        private String nbdAddr;
        private int port;

        public String getNbdAddr() {
            return nbdAddr;
        }

        public void setNbdAddr(String nbdAddr) {
            this.nbdAddr = nbdAddr;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    public static class CbdToNbdCmd extends VolumeCommand {
        private String portRange;

        public String getPortRange() {
            return portRange;
        }

        public void setPortRange(String portRange) {
            this.portRange = portRange;
        }
    }

    public static class CloneVolumeCmd extends VolumeCommand {
        private String dstVolume;

        public String getDstVolume() {
            return dstVolume;
        }

        public void setDstVolume(String dstVolume) {
            this.dstVolume = dstVolume;
        }
    }

    public static class CreateSnapshotCmd extends VolumeCommand {
        private String snapshot;
        private boolean skipOnExisting;

        public String getSnapshot() {
            return snapshot;
        }

        public void setSnapshot(String snapshot) {
            this.snapshot = snapshot;
        }

        public boolean isSkipOnExisting() {
            return skipOnExisting;
        }

        public void setSkipOnExisting(boolean skipOnExisting) {
            this.skipOnExisting = skipOnExisting;
        }
    }

    public static class DeleteVolumeCmd extends VolumeCommand {
        private boolean force;

        public boolean isForce() {
            return force;
        }

        public void setForce(boolean force) {
            this.force = force;
        }
    }

    public static class GetVolumeClientsCmd extends VolumeCommand {
    }

    public static class ClientInfo {
        public ClientInfo(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        private String ip;
        private int port;

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    public static class GetVolumeClientsRsp extends AgentResponse {
        private List<ClientInfo> clients;

        public List<ClientInfo> getClients() {
            return clients;
        }

        public void setClients(List<ClientInfo> clients) {
            this.clients = clients;
        }
    }

    public static class SizeCmd extends AgentCommand {
        private long size;
        private String unit;

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public long getSizeInBytes() {
            if (unit != null && unit.toLowerCase().startsWith("m")) {
                return SizeUnit.MEGABYTE.toByte(size);
            } else {
                return size;
            }
        }
    }

    public static class CreateVolumeCmd extends SizeCmd {
        private String logicalPool;
        private String volume;
        private boolean skipIfExisting;

        public String getLogicalPool() {
            return logicalPool;
        }

        public void setLogicalPool(String logicalPool) {
            this.logicalPool = logicalPool;
        }

        public String getVolume() {
            return volume;
        }

        public void setVolume(String volume) {
            this.volume = volume;
        }

        public boolean isSkipIfExisting() {
            return skipIfExisting;
        }

        public void setSkipIfExisting(boolean skipIfExisting) {
            this.skipIfExisting = skipIfExisting;
        }
    }

    public static class GetCapacityCmd extends AgentCommand {
        private List<String> logicalPoolNames;

        public List<String> getLogicalPoolNames() {
            return logicalPoolNames;
        }

        public void setLogicalPoolNames(List<String> logicalPoolNames) {
            this.logicalPoolNames = logicalPoolNames;
        }
    }

    public static class DeployClientCmd extends AgentCommand {
        private String ip;
        private Integer port;
        private String username;
        private String password;

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class GetFactsRsp extends AgentResponse {
        private String uuid;
        private String version;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    public static class GetFactsCmd extends AgentCommand {
    }

    public static class CheckHostStorageConnectionCmd extends VolumeCommand {
        public String hostUuid;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }
    }

    public static class CheckHostStorageConnectionRsp extends AgentResponse {
    }

    public static abstract class VolumeCommand extends AgentCommand {
        protected String path;

        public void setPath(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }

    public static abstract class VolumeResponse extends AgentResponse {
        protected String installPath;
        protected long size;
        protected long actualSize;

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }

        public String getInstallPath() {
            return installPath;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public long getActualSize() {
            return actualSize;
        }

        public void setActualSize(long actualSize) {
            this.actualSize = actualSize;
        }
    }

    public static class UpdateHostDependencyCmd extends AgentCommand {
        public String updatePackages;
        public String zstackRepo;
    }

    public static class UpdateHostDependencyRsp extends AgentResponse {
    }

    public static class VhostHostCmd extends AgentCommand {
        public String hostIp;
        public int sshPort;
        public String sshUsername;
        @NoLogging
        public String sshPassword;
    }

    public static class DeployVhostCmd extends VhostHostCmd {
    }

    public static class DestroyVhostCmd extends VhostHostCmd {
    }

    public static class PrepareVhostTargetEnvCmd extends AgentCommand {
    }

    public static class CreateVhostBdevCmd extends VhostHostCmd {
        public String logicalPool;
        public String volume;
        public String bdevName;
    }

    public static class CreateVhostBdevRsp extends AgentResponse {
        public String socketPath;
    }

    public static class DeleteVhostBdevCmd extends VhostHostCmd {
        public String bdevName;
    }

    public static class VhostTargetHealthCmd extends AgentCommand {
        public String containerName;
        public String controlSock;
    }

    public static class VhostTargetHealthRsp extends AgentResponse {
        public boolean targetRunning;
    }

    public static class VhostResizeCmd extends AgentCommand {
        public String bdevName;
        public long sizeMib;
        public String controlSock;
    }

    public static class AgentResponse extends ZbsMdsBase.AgentResponse {
    }

    public static class AgentCommand extends ZbsMdsBase.AgentCommand {
    }

    public ZbsStorageController(ExternalPrimaryStorageVO self) {
        this.self = self;
        this.reloadDbInfo();
    }

    public ZbsStorageController(String config) {
        this.self = new ExternalPrimaryStorageVO();
        this.self.setConfig(config);
        this.config = StringUtils.isEmpty(self.getConfig()) ? new Config() : JSONObjectUtil.toObject(self.getConfig(), Config.class);
        this.addonInfo = new AddonInfo();
    }
}
