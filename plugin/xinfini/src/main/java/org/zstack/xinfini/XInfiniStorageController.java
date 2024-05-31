package org.zstack.xinfini;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostInventory;
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
import org.zstack.storage.addon.primary.ExternalPrimaryStorageFactory;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.vhost.kvm.VhostVolumeTO;
import org.zstack.xinfini.sdk.MetadataState;
import org.zstack.xinfini.sdk.XInfiniClient;
import org.zstack.xinfini.sdk.XInfiniConnectConfig;
import org.zstack.xinfini.sdk.node.NodeModule;
import org.zstack.xinfini.sdk.pool.BsPolicyModule;
import org.zstack.xinfini.sdk.pool.PoolCapacity;
import org.zstack.xinfini.sdk.pool.PoolModule;
import org.zstack.xinfini.sdk.vhost.BdcBdevModule;
import org.zstack.xinfini.sdk.vhost.BdcModule;
import org.zstack.xinfini.sdk.volume.VolumeModule;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.storage.addon.primary.ExternalPrimaryStorageNameHelper.*;
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
    private  XInfiniAddonInfo addonInfo;
    private final XInfiniApiHelper apiHelper;
    @Autowired
    private ExternalPrimaryStorageFactory extPsFactory;

    // TODO static nqn
    private final static String hostNqn = "nqn.2014-08.org.nvmexpress:uuid:zstack";
    private final static String vhostSocketDir = "/var/run/bdc/";

    private static final StorageCapabilities capabilities = new StorageCapabilities();

    private final static long MIN_SIZE = 1024 * 1024 * 1024L;

    private final static long MAX_ISCSI_TARGET_LUN_COUNT = 64;

    static {
        VolumeSnapshotCapability scap = new VolumeSnapshotCapability();
        scap.setSupport(true);
        scap.setArrangementType(VolumeSnapshotCapability.VolumeSnapshotArrangementType.INDIVIDUAL);
        scap.setSupportCreateOnHypervisor(false);
        capabilities.setSnapshotCapability(scap);
        capabilities.setSupportCloneFromVolume(false);
        capabilities.setSupportStorageQos(true);
        capabilities.setSupportLiveExpandVolume(false);
        capabilities.setSupportedImageFormats(Collections.singletonList("raw"));
    }

    enum LunType {
        Volume,
        Snapshot
    }

    public XInfiniStorageController(ExternalPrimaryStorageVO self) {
        this(self.getConfig());
        this.self = self;
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
        } /*else if (VolumeProtocol.iSCSI.toString().equals(v.getProtocol())) {
            to = activeIscsiVolume(h, v, shareable);
            comp.success(to);
            return;
        }*/

        comp.fail(operr("not supported protocol[%s]", v.getProtocol()));
    }

    private ActiveVolumeTO activeVhostVolume(HostInventory h, BaseVolumeInfo vol) {
        String vhostName = buildBdevName(vol.getUuid());
        BdcModule bdc = apiHelper.queryBdcByIp(h.getManagementIp());
        VolumeModule volModule = getVolumeModule(vol);
        BdcBdevModule bdev = apiHelper.createBdcBdev(bdc.getSpec().getId(), volModule.getSpec().getId(), vhostName);

        VhostVolumeTO to = new VhostVolumeTO();
        to.setInstallPath(bdev.getSpec().getSocketPath());
        return to;
    }

    /*private synchronized ActiveVolumeTO activeIscsiVolume(HostInventory h, BaseVolumeInfo vol, boolean shareable) {
        String clientIqn = IscsiUtils.getHostInitiatorName(h.getUuid());
        if (clientIqn == null) {
            throw new RuntimeException(String.format("cannot get host[uuid:%s] initiator name", h.getUuid()));
        }
        return activeIscsiVolume(clientIqn, vol, shareable);
    }*/

    @Override
    public void deactivate(String installPath, String protocol, HostInventory h, Completion comp) {
        logger.debug(String.format("deactivating volume[path: %s, protocol:%s] on host[uuid:%s, ip:%s]",
                installPath, protocol, h.getUuid(), h.getManagementIp()));
        if (VolumeProtocol.Vhost.toString().equals(protocol)) {
            deactivateVhost(installPath, h);
            comp.success();
            return;
        } /*else if (VolumeProtocol.iSCSI.toString().equals(protocol)) {
            // iscsi target is shared by all hosts, we cannot control one volume on one host for now.
            deactivateIscsi(installPath, h);
            comp.success();
            return;
        }*/

        comp.fail(operr("not supported protocol[%s] for deactivate", protocol));
    }

    private void deactivateVhost(String installPath, HostInventory h) {
        int volId = getVolIdFromPath(installPath);
        VolumeModule vol = apiHelper.getVolume(volId);
        BdcModule bdc = apiHelper.queryBdcByIp(h.getManagementIp());
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

    }

    @Override
    public void blacklist(String installPath, String protocol, HostInventory h, Completion comp) {
        comp.success();
    }

    @Override
    public String getActivePath(BaseVolumeInfo v, HostInventory h, boolean shareable) {
        return null;
    }

    @Override
    public BaseVolumeInfo getActiveVolumeInfo(String activePath, HostInventory h, boolean shareable) {
        return null;
    }

    @Override
    public List<ActiveVolumeClient> getActiveClients(String installPath, String protocol) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getActiveVolumesLocation(HostInventory h) {
        return null;
    }

    @Override
    public void activateHeartbeatVolume(HostInventory h, ReturnValueCompletion<HeartbeatVolumeTO> comp) {

    }

    @Override
    public void deactivateHeartbeatVolume(HostInventory h, Completion comp) {

    }

    @Override
    public HeartbeatVolumeTO getHeartbeatVolumeActiveInfo(HostInventory h) {
        return null;
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

        List<PoolModule> pools = getSelfPools();
        long total = pools.stream().mapToLong(v -> apiHelper.getPoolCapacity(v).getTotalCapacity()).sum();
        long avail = pools.stream().mapToLong(v -> apiHelper.getPoolCapacity(v).getAvailableCapacity()).sum();
        StorageCapacity cap = new StorageCapacity();
        cap.setHealthy(getHealthy(pools));
        cap.setAvailableCapacity(avail);
        cap.setTotalCapacity(total);
        comp.success(cap);
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

    }

    private void setNodeHealthyByVhost(HostInventory host, NodeHealthy healthy) {

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
        Set<Integer> poolIds = addonInfo.getPools().stream().filter(it -> configPoolIds.contains(it.getId()))
                .map(XInfiniAddonInfo.Pool::getId).collect(Collectors.toSet());

        List<PoolModule> pools = apiHelper.queryPools();
        pools.removeIf(it -> !poolIds.contains(it.getSpec().getId()));
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
        // TODO allocate pool
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

        VolumeModule volModule = apiHelper.createVolume(v.getName(), poolId, SizeUnit.BYTE.toMegaByte(v.getSize()));
        VolumeStats stats = new VolumeStats();
        stats.setInstallPath(buildXInfiniPath(poolId, volModule.getSpec().getId()));
        stats.setSize(SizeUnit.MEGABYTE.toByte(volModule.getSpec().getSizeMb()));
        // TODO not support actualSize yet
        // stats.setActualSize(volModule.getDataSize());
        stats.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
        stats.setRunStatus(volModule.getMetadata().getState().getState());
        comp.success(stats);
    }

    @Override
    public void deleteVolume(String installPath, Completion comp) {
        int volId = getVolIdFromPath(installPath);
        apiHelper.deleteVolume(volId, true);
        comp.success();
    }

    @Override
    public void deleteVolumeAndSnapshot(String installPath, Completion comp) {

    }

    @Override
    public void trashVolume(String installPath, Completion comp) {
        // xinfini not support trash yet
        deleteVolume(installPath, comp);
    }

    @Override
    public void cloneVolume(String srcInstallPath, CreateVolumeSpec dst, ReturnValueCompletion<VolumeStats> comp) {

    }

    @Override
    public void copyVolume(String srcInstallPath, CreateVolumeSpec dst, ReturnValueCompletion<VolumeStats> comp) {

    }

    @Override
    public void flattenVolume(String installPath, ReturnValueCompletion<VolumeStats> comp) {
        // TODO flatten snapshot
        stats(installPath, comp);
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

    }

    @Override
    public void setVolumeQos(BaseVolumeInfo v, Completion comp) {

    }

    @Override
    public void export(ExportSpec espec, VolumeProtocol protocol, ReturnValueCompletion<RemoteTarget> comp) {

    }

    synchronized NvmeRemoteTarget exportNvmf(ExportSpec espec) {
        return null;
    }

    @Override
    public void unexport(ExportSpec espec, VolumeProtocol protocol, Completion comp) {

    }

    private synchronized void unexportNvmf(String source) {

    }

    @Override
    public void createSnapshot(CreateVolumeSnapshotSpec spec, ReturnValueCompletion<VolumeSnapshotStats> comp) {

    }

    @Override
    public void deleteSnapshot(String installPath, Completion comp) {
    }

    @Override
    public void expungeSnapshot(String installPath, Completion comp) {
    }

    @Override
    public void revertVolumeSnapshot(String snapshotInstallPath, ReturnValueCompletion<VolumeStats> comp) {

    }

    @Override
    public void validateConfig(String config) {

    }

    @Override
    public void setTrashExpireTime(int timeInSeconds, Completion completion) {

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
