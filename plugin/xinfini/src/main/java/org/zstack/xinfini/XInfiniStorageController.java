package org.zstack.xinfini;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.storage.addon.*;
import org.zstack.header.storage.addon.primary.*;
import org.zstack.header.storage.primary.VolumeSnapshotCapability;
import org.zstack.header.storage.snapshot.VolumeSnapshotStats;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeProtocol;
import org.zstack.header.volume.VolumeStats;
import org.zstack.header.xinfini.XInfiniConstants;
import org.zstack.iscsi.IscsiUtils;
import org.zstack.iscsi.kvm.IscsiHeartbeatVolumeTO;
import org.zstack.iscsi.kvm.IscsiVolumeTO;
import org.zstack.storage.addon.primary.ExternalPrimaryStorageFactory;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;
import org.zstack.vhost.kvm.VhostVolumeTO;
import org.zstack.xinfini.sdk.MetadataState;
import org.zstack.xinfini.sdk.XInfiniClient;
import org.zstack.xinfini.sdk.XInfiniConnectConfig;
import org.zstack.xinfini.sdk.iscsi.*;
import org.zstack.xinfini.sdk.node.NodeModule;
import org.zstack.xinfini.sdk.pool.BsPolicyModule;
import org.zstack.xinfini.sdk.pool.PoolCapacity;
import org.zstack.xinfini.sdk.pool.PoolModule;
import org.zstack.xinfini.sdk.vhost.BdcBdevModule;
import org.zstack.xinfini.sdk.vhost.BdcModule;
import org.zstack.xinfini.sdk.volume.VolumeModule;
import org.zstack.xinfini.sdk.volume.VolumeSnapshotModule;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.iscsi.IscsiUtils.getHostMnIpFromInitiatorName;
import static org.zstack.storage.addon.primary.ExternalPrimaryStorageNameHelper.buildImageName;
import static org.zstack.storage.addon.primary.ExternalPrimaryStorageNameHelper.buildVolumeName;
import static org.zstack.xinfini.XInfiniIscsiHelper.buildIscsiClientGroupName;
import static org.zstack.xinfini.XInfiniIscsiHelper.iscsiHeartbeatVolumeName;
import static org.zstack.xinfini.XInfiniPathHelper.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class XInfiniStorageController implements PrimaryStorageControllerSvc, PrimaryStorageNodeSvc {
    private static CLogger logger = Utils.getLogger(XInfiniStorageController.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    private ExternalPrimaryStorageVO self;
    private XInfiniConfig config;
    private XInfiniAddonInfo addonInfo;
    private final XInfiniApiHelper apiHelper;
    @Autowired
    private ExternalPrimaryStorageFactory extPsFactory;

    private final static String hostNqn = "nqn.2014-08.org.nvmexpress:uuid:zstack";
    private final String vhostSocketDir;

    private static final StorageCapabilities capabilities = new StorageCapabilities();

    private final static long MIN_SIZE = 1024 * 1024 * 1024L;

    static {
        VolumeSnapshotCapability scap = new VolumeSnapshotCapability();
        scap.setSupport(true);
        scap.setArrangementType(VolumeSnapshotCapability.VolumeSnapshotArrangementType.INDIVIDUAL);
        scap.setSupportCreateOnHypervisor(false);
        capabilities.setSnapshotCapability(scap);
        capabilities.setSupportCloneFromVolume(false);
        capabilities.setSupportStorageQos(false);
        capabilities.setSupportLiveExpandVolume(false);
        capabilities.setSupportExportVolumeSnapshot(false);
        capabilities.setSupportedImageFormats(Collections.singletonList("raw"));
    }

    enum LunType {
        Volume,
        Snapshot
    }

    public XInfiniStorageController(ExternalPrimaryStorageVO self) {
        this(self.getConfig());
        this.reloadDbInfo();
    }

    public XInfiniStorageController(String config) {
        XInfiniConfig xConfig = JSONObjectUtil.toObject(config, XInfiniConfig.class);
        // TODO add node connection check
        XInfiniConfig.Node node = xConfig.getNodes().get(0);
        XInfiniConnectConfig clientConfig = new XInfiniConnectConfig();
        clientConfig.hostname = node.getIp();
        clientConfig.port = node.getPort();
        clientConfig.readTimeout = TimeUnit.MINUTES.toMillis(10);
        clientConfig.writeTimeout = TimeUnit.MINUTES.toMillis(10);
        clientConfig.token = xConfig.getToken();
        XInfiniClient client = new XInfiniClient();
        client.configure(clientConfig);

        apiHelper = new XInfiniApiHelper(client);
        vhostSocketDir = String.format("/var/run/bdc-%s/", apiHelper.getClusterUuid());
    }

    private String protocolToString(VolumeProtocol protocol) {
        if (protocol == VolumeProtocol.NVMEoF) {
            return "nvmf";
        } else if (protocol == VolumeProtocol.Vhost) {
            return "vhost";
        } else if (protocol == VolumeProtocol.iSCSI) {
            return "iscsi";
        } else {
            throw new RuntimeException("not supported protocol " + protocol);
        }
    }

    @Override
    public void activate(BaseVolumeInfo v, HostInventory h, boolean shareable, ReturnValueCompletion<ActiveVolumeTO> comp) {
        ActiveVolumeTO to;
        if (VolumeProtocol.Vhost.toString().equals(v.getProtocol())) {
            to = activeVhostVolume(h, v);
            comp.success(to);
            return;
        } else if (VolumeProtocol.iSCSI.toString().equals(v.getProtocol())) {
            to = activeIscsiVolume(h, v, shareable);
            comp.success(to);
            return;
        }

        comp.fail(operr("not supported protocol[%s]", v.getProtocol()));
    }

    private ActiveVolumeTO activeVhostVolume(HostInventory h, BaseVolumeInfo vol) {
        String vhostName = buildBdevName(vol.getUuid());
        BdcModule bdc = apiHelper.queryBdcByIp(h.getManagementIp());
        VolumeModule volModule = getVolumeModule(vol);
        if (volModule == null) {
            throw new OperationFailureException(operr("cannot get volume[%s] details, maybe it has been deleted", vol.getInstallPath()));
        }

        BdcBdevModule bdev = apiHelper.createBdcBdev(bdc.getSpec().getId(), volModule.getSpec().getId(), vhostName);
        VhostVolumeTO to = new VhostVolumeTO();
        to.setInstallPath(bdev.getSpec().getSocketPath());
        return to;
    }

    private synchronized ActiveVolumeTO activeIscsiVolume(HostInventory h, BaseVolumeInfo vol, boolean shareable) {
        String clientIqn = IscsiUtils.getHostInitiatorName(h.getUuid());
        if (clientIqn == null) {
            throw new RuntimeException(String.format("cannot get host[uuid:%s] initiator name", h.getUuid()));
        }
        return activeIscsiVolume(h.getManagementIp(), clientIqn, vol, shareable);
    }

    public synchronized ActiveVolumeTO activeIscsiVolume(String clientIp, String clientIqn, BaseVolumeInfo vol, boolean shareable) {
        IscsiVolumeTO to = new IscsiVolumeTO();
        IscsiRemoteTarget target = createIscsiRemoteTarget(clientIp, clientIqn, vol.getInstallPath());
        to.setInstallPath(target.getResourceURI());
        return to;
    }

    @Override
    public void deactivate(String installPath, String protocol, HostInventory h, Completion comp) {
        logger.debug(String.format("deactivating volume[path: %s, protocol:%s] on host[uuid:%s, ip:%s]",
                installPath, protocol, h.getUuid(), h.getManagementIp()));
        if (VolumeProtocol.Vhost.toString().equals(protocol)) {
            deactivateVhost(installPath, h);
            comp.success();
            return;
        } else if (VolumeProtocol.iSCSI.toString().equals(protocol)) {
            // iscsi target is shared by all hosts, we cannot control one volume on one host for now.
            deactivateIscsi(installPath, h);
            comp.success();
            return;
        }

        comp.fail(operr("not supported protocol[%s] for deactivate", protocol));
    }

    private void deactivateIscsi(String installPath, HostInventory h) {
        String iqn = IscsiUtils.getHostInitiatorName(h.getUuid());
        if (iqn == null) {
            throw new RuntimeException(String.format("cannot get host[uuid:%s] initiator name", h.getUuid()));
        }
        deactivateIscsi(installPath, iqn);
    }

    public void deactivateIscsi(String installPath, String clientIqn) {
        int volId = getVolIdFromPath(installPath);
        IscsiClientModule client = apiHelper.queryIscsiClientByIqn(clientIqn);
        if (client == null) {
            logger.info(String.format("cannot find client with code %s, skip deactive", clientIqn));
            return;
        }

        IscsiClientGroupModule group = apiHelper.getIscsiClientGroup(client.getSpec().getIscsiClientGroupId());
        VolumeClientGroupMappingModule map = apiHelper.queryVolumeClientGroupMappingByGroupIdAndVolId(group.getSpec().getId(), volId);
        if (map == null) {
            logger.info(String.format("vol %s not related to client group %s, skip deactive", volId, group.getSpec().getId()));
            return;
        }

        retry(() -> apiHelper.deleteVolumeClientGroupMapping(map.getSpec().getId()));
    }

    private void deactivateVhost(String installPath, HostInventory h) {
        int volId = getVolIdFromPath(installPath);
        VolumeModule vol = apiHelper.getVolume(volId);
        BdcModule bdc = apiHelper.queryBdcByIp(h.getManagementIp(), false);
        if (bdc == null) {
            return;
        }

        BdcBdevModule bdev = apiHelper.queryBdcBdevByVolumeIdAndBdcId(vol.getSpec().getId(), bdc.getSpec().getId());
        if (bdev == null) {
            return;
        }

        retry(() -> apiHelper.deleteBdcBdev(bdev.getSpec().getId()));
    }

    @Override
    public void deactivate(String installPath, String protocol, ActiveVolumeClient client, Completion comp) {
        HostVO host = Q.New(HostVO.class).eq(HostVO_.managementIp, client.getManagerIp()).find();
        if (host != null) {
            deactivate(installPath, protocol, HostInventory.valueOf(host), comp);
        } else {
            // bm instance InitiatorName
            deactivateIscsi(installPath, client.getQualifiedName());
            comp.success();
        }
    }

    @Override
    public void blacklist(String installPath, String protocol, HostInventory h, Completion comp) {
        // todo
        comp.success();
    }

    @Override
    public String getActivePath(BaseVolumeInfo v, HostInventory h, boolean shareable) {
        if (VolumeProtocol.Vhost.toString().equals(v.getProtocol())) {
            String bdevName = buildBdevName(v.getUuid());
            BdcModule bdc = apiHelper.queryBdcByIp(h.getManagementIp());
            VolumeModule volModule = getVolumeModule(v);
            BdcBdevModule bdev = apiHelper.getOrCreateBdcBdevByVolumeIdAndBdcId(volModule.getSpec().getId(), bdc.getSpec().getId(), bdevName);
            return bdev.getSpec().getSocketPath();
        } else if (VolumeProtocol.iSCSI.toString().equals(v.getProtocol())) {
            if (v.getInstallPath().contains("@")) {
                // todo
                throw new OperationFailureException(operr("not support active snapshot with iscsi protocol"));
            }
            String clientIqn = IscsiUtils.getHostInitiatorName(h.getUuid());
            IscsiClientModule client = apiHelper.queryIscsiClientByIqn(clientIqn);
            IscsiClientGroupModule group = apiHelper.getIscsiClientGroup(client.getSpec().getIscsiClientGroupId());
            int volId = getVolIdFromPath(v.getInstallPath());
            VolumeClientGroupMappingModule map = apiHelper.queryVolumeClientGroupMappingByGroupIdAndVolId(group.getSpec().getId(), volId);
            if (map == null) {
                logger.info(String.format("vol %s not related to client group %s, skip get installPath", volId, group.getSpec().getId()));
                return null;
            }

            List<IscsiGatewayModule> iscsiGateways = apiHelper.queryIscsiGateways();
            List<IscsiGatewayClientGroupMappingModule> mappings = apiHelper.queryIscsiGatewayClientGroupMappingByGroupId(group.getSpec().getId());
            List<Integer> gatewayIds = mappings.stream()
                    .map(IscsiGatewayClientGroupMappingModule::getSpec)
                    .map(IscsiGatewayClientGroupMappingModule.IscsiGatewayClientGroupMappingSpec::getIscsiGatewayId)
                    .collect(Collectors.toList());
            List<IscsiGatewayModule> groupRelatedGateways = iscsiGateways
                    .stream()
                    .filter(it -> gatewayIds.contains(it.getSpec().getId()))
                    .collect(Collectors.toList());

            IscsiRemoteTarget target = new IscsiRemoteTarget();
            target.setPort(groupRelatedGateways.get(0).getSpec().getPort());
            target.setTransport("tcp");
            target.setIqn(client.getStatus().getTargetIqns().get(0));
            target.setIp(groupRelatedGateways.stream().map(it -> it.getSpec().getIps().get(0)).collect(Collectors.joining(",")));
            target.setDiskId(apiHelper.getVolume(volId).getSpec().getSerial());
            target.setDiskIdType(IscsiRemoteTarget.DiskIdType.serial.toString());
            return target.getResourceURI();
        }

        throw new OperationFailureException(operr("not supported protocol[%s]", v.getProtocol()));
    }

    @Override
    public BaseVolumeInfo getActiveVolumeInfo(String activePath, HostInventory h, boolean shareable) {
        BaseVolumeInfo info = new BaseVolumeInfo();
        String volUuid;
        if (activePath.startsWith(vhostSocketDir)) {
            volUuid = activePath.replace(String.format("%svolume-", vhostSocketDir), "");
            info.setUuid(volUuid);
            info.setProtocol(VolumeProtocol.Vhost.toString());
            info.setShareable(shareable);
        } else {
            // TODO support other protocols
            throw new OperationFailureException(operr("not supported get volume info from [%s]", activePath));
        }

        VolumeModule vol = apiHelper.queryVolumeByName(buildVolumeName(volUuid));
        if (vol == null) {
            return info;
        }

        info.setInstallPath(buildXInfiniPath(vol.getSpec().getPoolId(), vol.getSpec().getId()));
        return info;
    }

    @Override
    public List<ActiveVolumeClient> getActiveClients(String installPath, String protocol) {
        if (VolumeProtocol.Vhost.toString().equals(protocol)) {
            VolumeModule vol = apiHelper.getVolume(getVolIdFromPath(installPath));
            if (vol == null) {
                return Collections.emptyList();
            }

            List<BdcBdevModule> bdcBdevs = apiHelper.queryBdcBdevByVolumeId(vol.getSpec().getId());
            return bdcBdevs.stream().map(it -> {
                ActiveVolumeClient c = new ActiveVolumeClient();
                if (CoreGlobalProperty.UNIT_TEST_ON) {
                    c.setManagerIp("127.0.0.1");
                } else {
                    c.setManagerIp(it.getSpec().getNodeIp());
                }
                return c;
            }).collect(Collectors.toList());
        } else if (VolumeProtocol.iSCSI.toString().equals(protocol)) {
            VolumeModule vol = apiHelper.getVolume(getVolIdFromPath(installPath));
            if (vol == null) {
                return Collections.emptyList();
            }

            List<VolumeClientMappingModule> mappings = apiHelper.queryVolumeClientMappingByVolId(vol.getSpec().getId());
            if (mappings.isEmpty()) {
                return Collections.emptyList();
            }

            List<Integer> clientIds = mappings.stream().map(it -> it.getSpec().getIscsiClientId()).collect(Collectors.toList());
            List<IscsiClientModule> clients = apiHelper.queryIscsiClientByIds(clientIds);
            return clients.stream().map(it -> {
                String clientIqn = it.getSpec().getCode();
                ActiveVolumeClient c = new ActiveVolumeClient();
                if (clientIqn.contains("iqn")) {
                    c.setQualifiedName(clientIqn);
                    c.setManagerIp(getHostMnIpFromInitiatorName(clientIqn));
                } else {
                    c.setManagerIp(clientIqn);
                }

                return c;
            }).collect(Collectors.toList());
        } else {
            throw new OperationFailureException(operr("not supported protocol[%s] for active", protocol));
        }
    }

    @Override
    public List<String> getActiveVolumesLocation(HostInventory h) {
        return Collections.singletonList("file://" + PathUtil.join(vhostSocketDir, "volume-*"));
    }

    @Override
    public synchronized void activateHeartbeatVolume(HostInventory h, ReturnValueCompletion<HeartbeatVolumeTO> comp) {
        String clientIqn = IscsiUtils.getHostInitiatorName(h.getUuid());
        if (clientIqn == null) {
            throw new RuntimeException(String.format("cannot get host[uuid:%s] initiator name", h.getUuid()));
        }
        VolumeModule heartbeatVol = apiHelper.queryVolumeByName(iscsiHeartbeatVolumeName);
        if (heartbeatVol == null) {
            long size = SizeUnit.GIGABYTE.toMegaByte(2);
            heartbeatVol = apiHelper.createVolume(iscsiHeartbeatVolumeName, allocateFreePool(size).getSpec().getId(), size);
        }
        IscsiRemoteTarget target = createIscsiRemoteTarget(h.getManagementIp(), clientIqn, buildXInfiniPath(heartbeatVol.getSpec().getPoolId(), heartbeatVol.getSpec().getId()));

        IscsiHeartbeatVolumeTO to = new IscsiHeartbeatVolumeTO();
        to.setInstallPath(target.getResourceURI());
        to.setHostId(apiHelper.queryBdcByIp(h.getManagementIp()).getSpec().getId());
        to.setHeartbeatRequiredSpace(SizeUnit.MEGABYTE.toByte(1));
        to.setCoveringPaths(Collections.singletonList(vhostSocketDir));
        comp.success(to);
    }

    @Override
    public void deactivateHeartbeatVolume(HostInventory h, Completion comp) {
        VolumeModule heartbeatVol = apiHelper.queryVolumeByName(iscsiHeartbeatVolumeName);
        if (heartbeatVol == null) {
            comp.success();
            return;
        }

        String clientIqn = IscsiUtils.getHostInitiatorName(h.getUuid());
        IscsiClientModule client = apiHelper.queryIscsiClientByIqn(clientIqn);
        if (client == null) {
            logger.info(String.format("cannot find client with code %s, skip deactive heartbeat volume", clientIqn));
            return;
        }

        IscsiClientGroupModule group = apiHelper.getIscsiClientGroup(client.getSpec().getIscsiClientGroupId());
        VolumeClientGroupMappingModule map = apiHelper.queryVolumeClientGroupMappingByGroupIdAndVolId(group.getSpec().getId(), heartbeatVol.getSpec().getId());
        if (map == null) {
            logger.info(String.format("vol %s not related to client group %s, skip deactive heartbeat volume", heartbeatVol.getSpec().getId(), group.getSpec().getId()));
            return;
        }

        retry(() -> apiHelper.deleteVolumeClientGroupMapping(map.getSpec().getId()));
    }

    @Override
    public HeartbeatVolumeTO getHeartbeatVolumeActiveInfo(HostInventory h) {
        VolumeModule heartbeatVol = apiHelper.queryVolumeByName(iscsiHeartbeatVolumeName);
        if (heartbeatVol == null) {
            throw new RuntimeException("heartbeat volume not found");
        }

        String clientIqn = IscsiUtils.getHostInitiatorName(h.getUuid());
        if (clientIqn == null) {
            throw new RuntimeException(String.format("cannot get host[uuid:%s] initiator name", h.getUuid()));
        }

        IscsiHeartbeatVolumeTO to = new IscsiHeartbeatVolumeTO();
        IscsiRemoteTarget target = createIscsiRemoteTarget(h.getManagementIp(), clientIqn, buildXInfiniPath(heartbeatVol.getSpec().getPoolId(), heartbeatVol.getSpec().getId()));
        to.setInstallPath(target.getResourceURI());
        to.setHostId(apiHelper.queryBdcByIp(h.getManagementIp()).getSpec().getId());
        to.setHeartbeatRequiredSpace(SizeUnit.MEGABYTE.toByte(1));
        to.setCoveringPaths(Collections.singletonList(vhostSocketDir));
        return to;
    }

    private VolumeModule getVolumeModule(BaseVolumeInfo vol) {
        if (vol.getInstallPath() != null) {
            int volId = getVolIdFromPath(vol.getInstallPath());
            return apiHelper.getVolume(volId);
        }

        if ("image".equals(vol.getType())) {
            return apiHelper.queryVolumeByName(buildImageName(vol.getUuid()));
        } else {
            return apiHelper.queryVolumeByName(buildVolumeName(vol.getUuid()));
        }
    }

    @Override
    public String getIdentity() {
        return XInfiniConstants.IDENTITY;
    }

    @Override
    public void connect(String config, String url, ReturnValueCompletion<LinkedHashMap> comp) {
        DebugUtils.Assert(StringUtils.isNotEmpty(config), "config cannot be none");
        XInfiniAddonInfo info = new XInfiniAddonInfo();
        XInfiniConfig xConfig = JSONObjectUtil.toObject(config, XInfiniConfig.class);

        List<NodeModule> nodes = apiHelper.queryNodes();
        if (CollectionUtils.isEmpty(nodes)) {
            comp.fail(operr("no node found"));
            return;
        }

        xConfig.getNodes().forEach(it -> nodes.stream()
                .filter(it1 -> it1.getSpec().getAdminIp().equals(it.getIp()) && it1.getSpec().isRoleAfaAdmin())
                .findAny()
                .orElseThrow(() -> new OperationFailureException(operr("fail to get node %s details, check ip address and role config", it.getIp()))));
        info.setNodes(nodes.stream().map(XInfiniAddonInfo.Node::valueOf).collect(Collectors.toList()));

        List<PoolModule> pools = apiHelper.queryPools();
        if (CollectionUtils.isEmpty(pools)) {
            comp.fail(operr("no pool found"));
            return;
        }

        if (!CollectionUtils.isEmpty(xConfig.getPools())) {
            xConfig.getPools().forEach(it -> pools.stream()
                    .filter(it1 -> it1.getSpec().getId() == it.getId())
                    .findAny()
                    .orElseThrow(() -> new OperationFailureException(operr("fail to get pool[id:%d, name:%s] %s details", it.getId(), it.getName()))));
        }
        info.setPools(pools.stream().map(this::getPoolAddonInfo).collect(Collectors.toList()));

        comp.success(JSONObjectUtil.rehashObject(info, LinkedHashMap.class));
    }

    private XInfiniAddonInfo.Pool getPoolAddonInfo(PoolModule pool) {
        BsPolicyModule bsPolicy = apiHelper.getBsPolicy(pool.getSpec().getDefaultBsPolicyId());
        PoolCapacity capacity = apiHelper.getPoolCapacity(pool);
        return XInfiniAddonInfo.Pool.valueOf(pool, bsPolicy, capacity);
    }

    private void reloadDbInfo() {
        self = dbf.reload(self);
        addonInfo = StringUtils.isEmpty(self.getAddonInfo()) ? new XInfiniAddonInfo() : JSONObjectUtil.toObject(self.getAddonInfo(), XInfiniAddonInfo.class);
        config = StringUtils.isEmpty(self.getConfig()) ? new XInfiniConfig() : JSONObjectUtil.toObject(self.getConfig(), XInfiniConfig.class);
    }

    @Override
    public void reportCapacity(ReturnValueCompletion<StorageCapacity> comp) {
        reloadDbInfo();
        List<XInfiniAddonInfo.Pool> pools = refreshPoolCapacity()
                .stream()
                .filter(it -> config.getPoolIds().contains(it.getId()))
                .collect(Collectors.toList());

        long total = pools.stream().mapToLong(XInfiniAddonInfo.Pool::getTotalCapacity).sum();
        long avail = pools.stream().mapToLong(XInfiniAddonInfo.Pool::getAvailableCapacity).sum();
        StorageCapacity cap = new StorageCapacity();
        cap.setHealthy(getHealthy(getSelfPools()));
        cap.setAvailableCapacity(avail);
        cap.setTotalCapacity(total);
        comp.success(cap);
    }

    private List<XInfiniAddonInfo.Pool> refreshPoolCapacity() {
        addonInfo.setPools(apiHelper.queryPools().stream().map(this::getPoolAddonInfo).collect(Collectors.toList()));
        SQL.New(ExternalPrimaryStorageVO.class).eq(ExternalPrimaryStorageVO_.uuid, self.getUuid())
                .set(ExternalPrimaryStorageVO_.addonInfo, JSONObjectUtil.toJsonString(addonInfo))
                .update();

        return addonInfo.getPools();
    }

    private StorageHealthy getHealthy(List<PoolModule> pools) {
        if (pools.stream().allMatch(it -> it.getMetadata().getState().getState().equals(MetadataState.active.toString()))) {
            return StorageHealthy.Ok;
        } else if (pools.stream().noneMatch(it -> it.getMetadata().getState().getState().equals(MetadataState.active.toString()))) {
            return StorageHealthy.Failed;
        } else {
            return StorageHealthy.Warn;
        }
    }

    @Override
    public void reportHealthy(ReturnValueCompletion<StorageHealthy> comp) {
        self = dbf.reload(self);
        addonInfo = JSONObjectUtil.toObject(self.getAddonInfo(), XInfiniAddonInfo.class);
        config = JSONObjectUtil.toObject(self.getConfig(), XInfiniConfig.class);

        List<PoolModule> pools = getSelfPools();
        comp.success(getHealthy(pools));
    }

    @Override
    public void reportNodeHealthy(HostInventory host, ReturnValueCompletion<NodeHealthy> comp) {
        String hostProtocol = getProtocolByHypervisorType(host.getHypervisorType());
        NodeHealthy healthy = new NodeHealthy();
        if (VolumeProtocol.Vhost.toString().equals(hostProtocol)) {
            setNodeHealthyByVhost(host, healthy);
        }

        if (VolumeProtocol.iSCSI.toString().equals(hostProtocol)) {
            setNodeHealthyByIscsi(host, healthy);
        }
        comp.success(healthy);
    }

    private void setNodeHealthyByIscsi(HostInventory host, NodeHealthy healthy) {
        VolumeProtocol protocol = VolumeProtocol.iSCSI;
        IscsiClientModule client = apiHelper.queryIscsiClientByIqn(IscsiUtils.getHostInitiatorName(host.getUuid()));
        if (client != null && client.getMetadata().getState().getState().equals(MetadataState.active.toString())) {
            healthy.setHealthy(protocol, StorageHealthy.Ok);
        }  else {
            healthy.setHealthy(protocol, StorageHealthy.Failed);
        }
    }

    private void setNodeHealthyByVhost(HostInventory host, NodeHealthy healthy) {
        VolumeProtocol protocol = VolumeProtocol.Vhost;
        BdcModule bdc = apiHelper.queryBdcByIp(host.getManagementIp());
        if (bdc.getMetadata().getState().getState().equals(MetadataState.active.toString())) {
            healthy.setHealthy(protocol, StorageHealthy.Ok);
        }  else {
            healthy.setHealthy(protocol, StorageHealthy.Failed);
        }
    }

    private String getProtocolByHypervisorType(String type) {
        NodeHealthyCheckProtocolExtensionPoint point = extPsFactory.nodeHealthyCheckProtocolExtensions.get(type);
        if (point != null) {
            return point.getHealthyProtocol();
        }
        return VolumeProtocol.Vhost.toString();
    }

    private List<PoolModule> getSelfPools() {
        Set<Integer> configPoolIds = config.getPoolIds();

        List<PoolModule> pools = apiHelper.queryPools();
        pools.removeIf(it -> !configPoolIds.contains(it.getSpec().getId()));
        return pools;
    }

    private int getPoolId(String name) {
        return addonInfo.getPools().stream().filter(it -> it.getName().equals(name)).findFirst()
                .orElseThrow(() -> new RuntimeException(String.format("cannot find pool[name:%s]", name))).getId();
    }

    @Override
    public StorageCapabilities reportCapabilities() {
        return capabilities;
    }

    @Override
    public String allocateSpace(AllocateSpaceSpec aspec) {
        PoolModule pool = allocateFreePool(aspec.getSize());
        if (pool == null) {
            throw new OperationFailureException(operr("no available pool with enough space[%d] and healthy status", aspec.getSize()));
        }

        return buildXInfiniPath(pool.getSpec().getId(), null);
    }

    private PoolModule allocateFreePool(long size) {
        List<PoolModule> pools = getSelfPools();
        return pools.stream()
                .filter(it -> {
                 PoolCapacity poolCapacity = apiHelper.getPoolCapacity(it);
                 return it.getMetadata().getState().getState().equals(MetadataState.active.toString()) && poolCapacity.getAvailableCapacity() > size;})
                .findAny()
                .orElse(null);
    }

    @Override
    public void createVolume(CreateVolumeSpec v, ReturnValueCompletion<VolumeStats> comp) {
        int poolId;
        v.setSize(Math.max(v.getSize(), MIN_SIZE));

        if (v.getAllocatedUrl() == null) {
            PoolModule pool = allocateFreePool(v.getSize());
            if (pool == null) {
                comp.fail(operr("no available pool with enough space[%d] and healthy status", v.getSize()));
                return;
            }
            poolId = pool.getSpec().getId();
        } else {
            poolId = getPoolIdFromPath(v.getAllocatedUrl());
        }

        VolumeModule volModule = apiHelper.createVolume(v.getName(), poolId, convertBytesToMegaBytes(v.getSize()));
        VolumeStats stats = new VolumeStats();
        stats.setInstallPath(buildXInfiniPath(poolId, volModule.getSpec().getId()));
        stats.setSize(SizeUnit.MEGABYTE.toByte(volModule.getSpec().getSizeMb()));
        // TODO not support actualSize yet
        // stats.setActualSize(volModule.getDataSize());
        stats.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
        stats.setRunStatus(volModule.getMetadata().getState().getState());
        comp.success(stats);
    }

    private long convertBytesToMegaBytes(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("Byte count cannot be negative");
        }
        
        return (long) Math.ceil(bytes / (1024.0 * 1024.0));
    }

    @Override
    public void deleteVolume(String installPath, Completion comp) {
        int volId = getVolIdFromPath(installPath);
        apiHelper.deleteVolume(volId, true);
        comp.success();
    }

    @Override
    public void deleteVolumeAndSnapshot(String installPath, Completion comp) {
        int volId = getVolIdFromPath(installPath);
        for (VolumeSnapshotModule mod : apiHelper.queryVolumeSnapshotByVolumeId(volId)) {
            apiHelper.deleteVolumeSnapshot(mod.getSpec().getId());
        }

        apiHelper.deleteVolume(volId, true);
        comp.success();
    }

    @Override
    public void trashVolume(String installPath, Completion comp) {
        // xinfini not support trash yet
        deleteVolume(installPath, comp);
    }

    @Override
    public void cloneVolume(String srcInstallPath, CreateVolumeSpec dst, ReturnValueCompletion<VolumeStats> comp) {
        int snapId = getSnapIdFromPath(srcInstallPath);
        VolumeModule vol = apiHelper.cloneVolume(snapId, dst.getName(), null, false);

        if (SizeUnit.MEGABYTE.toByte(vol.getSpec().getSizeMb()) < dst.getSize()) {
            vol = apiHelper.expandVolume(vol.getSpec().getId(), convertBytesToMegaBytes(dst.getSize()));
        }
        // TODO support expand volume size
        VolumeStats stats = new VolumeStats();
        stats.setInstallPath(buildXInfiniPath(getPoolIdFromPath(srcInstallPath), vol.getSpec().getId()));
        stats.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
        stats.setSize(SizeUnit.MEGABYTE.toByte(vol.getSpec().getSizeMb()));
        // TODO not support actualSize yet
        // stats.setActualSize(volModule.getDataSize());
        comp.success(stats);
    }

    @Override
    public void copyVolume(String srcInstallPath, CreateVolumeSpec dst, ReturnValueCompletion<VolumeStats> comp) {
        // TODO
        throw new OperationFailureException(operr("not support copy volume yet"));
    }

    @Override
    public void flattenVolume(String installPath, ReturnValueCompletion<VolumeStats> comp) {
        // TODO
        throw new OperationFailureException(operr("not support flatten volume yet"));
    }

    @Override
    public void stats(String installPath, ReturnValueCompletion<VolumeStats> comp) {
        VolumeModule vol = apiHelper.getVolume(getVolIdFromPath(installPath));
        VolumeStats stats = new VolumeStats();
        stats.setInstallPath(installPath);
        stats.setSize(SizeUnit.MEGABYTE.toByte(vol.getSpec().getSizeMb()));
        // TODO: not support actual yet
        stats.setActualSize(SizeUnit.MEGABYTE.toByte(vol.getSpec().getSizeMb()));
        stats.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
        comp.success(stats);
    }

    @Override
    public void batchStats(Collection<String> installPath, ReturnValueCompletion<List<VolumeStats>> comp) {
        List<VolumeStats> stats = installPath.stream().map(it -> {
            VolumeModule vol = apiHelper.getVolume(getVolIdFromPath(it));
            VolumeStats s = new VolumeStats();
            s.setInstallPath(it);
            s.setSize(SizeUnit.MEGABYTE.toByte(vol.getSpec().getSizeMb()));
            // TODO: not support actual yet
            s.setActualSize(SizeUnit.MEGABYTE.toByte(vol.getSpec().getSizeMb()));
            s.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
            return s;
        }).collect(Collectors.toList());
        comp.success(stats);
    }

    @Override
    public void expandVolume(String installPath, long size, ReturnValueCompletion<VolumeStats> comp) {
        int id = getVolIdFromPath(installPath);
        List<BdcBdevModule> bdcBdevs = apiHelper.queryBdcBdevByVolumeId(id);
        if (!bdcBdevs.isEmpty()) {
            comp.fail(operr("volume has related bdevs, not support live expand yet"));
            return;
        }

        VolumeModule vol = apiHelper.expandVolume(id, convertBytesToMegaBytes(size));
        VolumeStats stats = new VolumeStats();
        stats.setInstallPath(installPath);
        stats.setSize(SizeUnit.MEGABYTE.toByte(vol.getSpec().getSizeMb()));
        comp.success(stats);
    }

    @Override
    public void setVolumeQos(BaseVolumeInfo v, Completion comp) {
        // TODO support qos
        throw new OperationFailureException(operr("not support set volume qos yet"));
    }

    @Override
    public void deleteVolumeQos(BaseVolumeInfo v, Completion comp) {
        // TODO support qos
        throw new OperationFailureException(operr("not support set volume qos yet"));
    }

    @Override
    public void export(ExportSpec espec, VolumeProtocol protocol, ReturnValueCompletion<RemoteTarget> comp) {
        if (protocol == VolumeProtocol.NVMEoF) {
            // TODO
            comp.fail(operr("not support export nvmeof yet"));
        } else if (protocol == VolumeProtocol.iSCSI) {
            IscsiRemoteTarget target = exportIscsi(espec);
            comp.success(target);
        } else {
            throw new RuntimeException("unsupport target " + protocol.name());
        }
    }


    synchronized IscsiRemoteTarget exportIscsi(ExportSpec espec) {
        return createIscsiRemoteTarget(espec.getClientMnIp(), espec.getClientQualifiedName(), espec.getInstallPath());
    }

    private IscsiRemoteTarget createIscsiRemoteTarget(String clientIp, String clientIqn, String installPath) {
        if (installPath.contains("@")) {
            // todo
            throw new OperationFailureException(operr("not support active snapshot with iscsi protocol"));
        }

        int volId = getVolIdFromPath(installPath);
        IscsiClientGroupModule group;
        IscsiClientModule clientModule;
        clientModule = apiHelper.queryIscsiClientByIqn(clientIqn);
        if (clientModule != null) {
            group = apiHelper.getIscsiClientGroup(clientModule.getSpec().getIscsiClientGroupId());
            logger.info(String.format("iscsi client[code:%s] exist, use related client group[name:%s]",
                    clientModule.getSpec().getCode(), group.getSpec().getName()));
        } else {
            // create group and client together
            List<IscsiGatewayModule> iscsiGateways = apiHelper.queryIscsiGateways();
            List<Integer> gatewayIds = iscsiGateways.stream()
                    .map(IscsiGatewayModule::getSpec)
                    .map(IscsiGatewayModule.IscsiGatewaySpec::getId)
                    .collect(Collectors.toList());

            group = apiHelper.createIscsiClientGroup(buildIscsiClientGroupName(clientIp), gatewayIds, Collections.singletonList(clientIqn));
        }

        apiHelper.addVolumeClientGroupMapping(volId, group.getSpec().getId());
        List<IscsiGatewayClientGroupMappingModule> mappings = apiHelper.queryIscsiGatewayClientGroupMappingByGroupId(group.getSpec().getId());
        List<Integer> gatewayIds = mappings.stream()
                .map(IscsiGatewayClientGroupMappingModule::getSpec)
                .map(IscsiGatewayClientGroupMappingModule.IscsiGatewayClientGroupMappingSpec::getIscsiGatewayId)
                .collect(Collectors.toList());
        List<IscsiGatewayModule> groupRelatedGateways = apiHelper.queryIscsiGatewaysByIds(gatewayIds);

        // refresh client
        clientModule = apiHelper.queryIscsiClientByIqn(clientIqn);

        IscsiRemoteTarget target = new IscsiRemoteTarget();
        target.setPort(groupRelatedGateways.get(0).getSpec().getPort());
        target.setTransport("tcp");
        target.setIqn(clientModule.getStatus().getTargetIqns().get(0));
        target.setIp(groupRelatedGateways.stream().map(it -> it.getSpec().getIps().get(0)).collect(Collectors.joining(",")));
        target.setDiskId(apiHelper.getVolume(volId).getSpec().getSerial());
        target.setDiskIdType(IscsiRemoteTarget.DiskIdType.serial.toString());
        return target;
    }

    @Override
    public void unexport(ExportSpec espec, VolumeProtocol protocol, Completion comp) {
        if (protocol == VolumeProtocol.NVMEoF) {
            comp.fail(operr("not support unexport nvmeof yet"));
        } else if (protocol == VolumeProtocol.iSCSI) {
            unexportIscsi(espec.getInstallPath(), espec.getClientQualifiedName());
        } else {
            comp.fail(operr("unsupported protocol %s", protocol.name()));
            return;
        }
        comp.success();
    }

    private synchronized void unexportIscsi(String source, String clientIqn) {
        if (source.contains("@")) {
            // todo
            throw new OperationFailureException(operr("not support unexport snapshot with iscsi protocol"));
        }

        IscsiClientModule clientModule = apiHelper.queryIscsiClientByIqn(clientIqn);
        if (clientModule == null) {
            return;
        }

        VolumeClientGroupMappingModule mapping = apiHelper.queryVolumeClientGroupMappingByGroupIdAndVolId(clientModule.getSpec().getIscsiClientGroupId(), getVolIdFromPath(source));
        if (mapping == null) {
            return;
        }

        retry(() -> apiHelper.deleteVolumeClientGroupMapping(mapping.getSpec().getId()));
    }

    @Override
    public void createSnapshot(CreateVolumeSnapshotSpec spec, ReturnValueCompletion<VolumeSnapshotStats> comp) {
        VolumeSnapshotModule snapshot = apiHelper.createVolumeSnapshot(getVolIdFromPath(spec.getVolumeInstallPath()), spec.getName());
        VolumeSnapshotStats stats = new VolumeSnapshotStats();
        stats.setInstallPath(buildXInfiniSnapshotPath(spec.getVolumeInstallPath(), snapshot.getSpec().getId()));
        stats.setActualSize(snapshot.getSpec().getSizeMb());
        comp.success(stats);
    }

    @Override
    public void deleteSnapshot(String installPath, Completion comp) {
        apiHelper.deleteVolumeSnapshot(getSnapIdFromPath(installPath));
        comp.success();
    }

    @Override
    public void expungeSnapshot(String installPath, Completion comp) {
        apiHelper.deleteVolume(getSnapIdFromPath(installPath), true);
        comp.success();
    }

    @Override
    public void revertVolumeSnapshot(String snapshotInstallPath, ReturnValueCompletion<VolumeStats> comp) {
        // TODO
        throw new OperationFailureException(operr("not support revert volume snapshot yet"));
    }

    @Override
    public void validateConfig(String config) {

    }

    @Override
    public void setTrashExpireTime(int timeInSeconds, Completion completion) {
        //TODO
    }

    public void cleanActiveRecord(VolumeInventory vol) {

    }
    private void retry(Runnable r) {
        retry(r, 3);
    }


    private void retry(Runnable r, int retry) {
        while (retry-- > 0) {
            try {
                r.run();
                return;
            } catch (Exception e) {
                logger.warn("runnable failed, try ", e);
                try {
                    TimeUnit.SECONDS.sleep(3);
                } catch (InterruptedException ignore) {}
            }
        }
    }
}
