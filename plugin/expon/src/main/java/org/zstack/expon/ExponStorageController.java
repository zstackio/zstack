package org.zstack.expon;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.expon.sdk.ExponClient;
import org.zstack.expon.sdk.ExponConfig;
import org.zstack.expon.sdk.cluster.TianshuClusterModule;
import org.zstack.expon.sdk.nvmf.NvmfClientGroupModule;
import org.zstack.expon.sdk.nvmf.NvmfModule;
import org.zstack.expon.sdk.pool.FailureDomainModule;
import org.zstack.expon.sdk.uss.UssGatewayModule;
import org.zstack.expon.sdk.vhost.VHostControllerModule;
import org.zstack.expon.sdk.volume.VolumeModule;
import org.zstack.expon.sdk.volume.VolumeQos;
import org.zstack.expon.sdk.volume.VolumeSnapshotModule;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.expon.HealthStatus;
import org.zstack.header.host.HostInventory;
import org.zstack.header.storage.addon.NvmeRemoteTarget;
import org.zstack.header.storage.addon.RemoteTarget;
import org.zstack.header.storage.addon.StorageCapacity;
import org.zstack.header.storage.addon.StorageHealthy;
import org.zstack.header.storage.addon.primary.*;
import org.zstack.header.storage.primary.VolumeSnapshotCapability;
import org.zstack.header.storage.snapshot.VolumeSnapshotStats;
import org.zstack.header.volume.*;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.vhost.kvm.VHostVolumeTO;

import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.expon.ExponNameHelper.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ExponStorageController implements PrimaryStorageControllerSvc, PrimaryStorageNodeSvc {

    @Autowired
    private DatabaseFacade dbf;
    private ExternalPrimaryStorageVO self;
    private ExponAddonInfo addonInfo;

    private ExponApiHelper apiHelper;

    // TODO static nqn
    private static String hostNqn = "nqn.2014-08.org.nvmexpress:uuid:zstack";

    private static final StorageCapabilities capabilities = new StorageCapabilities();

    private static long MIN_SIZE = 1024 * 1024 * 1024L;

    {
        VolumeSnapshotCapability scap = new VolumeSnapshotCapability();
        scap.setSupport(true);
        scap.setArrangementType(VolumeSnapshotCapability.VolumeSnapshotArrangementType.INDIVIDUAL);
        capabilities.setSnapshotCapability(scap);
        capabilities.setSupportCloneFromVolume(false);
        capabilities.setSupportedImageFormats(Collections.singletonList("qcow2"));
    }

    public ExponStorageController(ExternalPrimaryStorageVO self) {
        this(self.getUrl());
        this.self = self;
    }

    public ExponStorageController(String url) {
        URI uri = URI.create(url);

        ExponConfig clientConfig = new ExponConfig();
        clientConfig.hostname = uri.getHost();
        clientConfig.port = uri.getPort();
        clientConfig.readTimeout = 10000L;
        clientConfig.writeTimeout = 10000L;
        ExponClient client = new ExponClient();
        client.configure(clientConfig);

        AccountInfo accountInfo = new AccountInfo();
        accountInfo.username = uri.getUserInfo().split(":")[0];
        accountInfo.password = uri.getUserInfo().split(":")[1];

        apiHelper = new ExponApiHelper(accountInfo, client);
        apiHelper.login();
    }

    @Override
    public void getVolumeStats(VolumeInventory vol, ReturnValueCompletion<VolumeStats> comp) {
        VolumeModule exponVol = apiHelper.queryVolume(buildVolumeName(vol.getUuid()));
        if (exponVol == null) {
            comp.fail(operr("cannot find volume[%s] in expon", vol.getUuid()));
            return;
        }

        VolumeStats stats = new VolumeStats();
        stats.setInstallPath(buildExponPath(exponVol.getPoolName(), exponVol.getId()));
        stats.setSize(exponVol.getVolumeSize());
        stats.setActualSize(exponVol.getDataSize());
        comp.success(stats);
    }

    private UssGatewayModule getUssGateway(VolumeProtocol protocol, String managerIp) {
        String protocolStr;
        if (protocol == VolumeProtocol.NVMEoF) {
            protocolStr = "nvmf";
        } else if (protocol == VolumeProtocol.VHost) {
            protocolStr = "vhost";
        } else if (protocol == VolumeProtocol.iSCSI) {
            protocolStr = "iscsi";
        } else {
            throw new RuntimeException("not supported protocol " + protocol);
        }

        UssGatewayModule uss = apiHelper.getUssGateway(buildUssGwName(protocolStr, managerIp));
        if (uss == null) {
            throw new RuntimeException(String.format("cannot find uss gateway for manager ip[%s] and protocol[%s]", managerIp, protocol));
        }

        return uss;
    }

    private VHostControllerModule getVhostController(String name) {
        return apiHelper.getVhostController(name);
    }

    @Override
    public void activate(BaseVolumeInfo v, HostInventory h, boolean shareable, ReturnValueCompletion<ActiveVolumeTO> comp) {
        ActiveVolumeTO to;
        if (v.getProtocol() == VolumeProtocol.VHost) {
            to = activeVhostVolume(h, v);
            comp.success(to);
            return;
        }

        comp.fail(operr("not supported protocol[%s]", v.getProtocol()));
    }


    private ActiveVolumeTO activeVhostVolume(HostInventory h, BaseVolumeInfo vol) {
        String vhostName = buildVhostControllerName(vol.getUuid());

        UssGatewayModule uss = getUssGateway(VolumeProtocol.VHost, h.getManagementIp());
        VHostControllerModule vhost = getVhostController(vhostName);
        VolumeModule exponVol = apiHelper.queryVolume(buildVolumeName(vol.getUuid()));

        if (vhost == null) {
            vhost = apiHelper.createVHostController(vhostName);
        }

        if (exponVol == null) {
            throw new RuntimeException("volume not found");
        }

        apiHelper.addVhostVolumeToUss(exponVol.getId(), vhost.getId(), uss.getId());
        VHostVolumeTO to = new VHostVolumeTO();
        to.setInstallPath(vhost.getPath());
        return to;

    }

    @Override
    public ActiveVolumeTO getActiveResult(BaseVolumeInfo v, boolean shareable) {
        String vhostName = buildVhostControllerName(v.getUuid());
        VHostControllerModule vhost = getVhostController(vhostName);
        VHostVolumeTO to = new VHostVolumeTO();
        to.setInstallPath(vhost.getPath());
        return to;
    }

    @Override
    public void deactivate(BaseVolumeInfo vol, HostInventory h, Completion comp) {
        if (vol.getProtocol() == VolumeProtocol.VHost) {
            deactiveVhost(vol, h);
            comp.success();
            return;
        }

        comp.fail(operr("not supported protocol[%s]", vol.getProtocol()));
    }

    private void deactiveVhost(BaseVolumeInfo vol, HostInventory h) {
        String vhostName = buildVhostControllerName(vol.getUuid());

        UssGatewayModule uss = getUssGateway(VolumeProtocol.VHost, h.getManagementIp());
        VHostControllerModule vhost = getVhostController(vhostName);
        VolumeModule exponVol = apiHelper.queryVolume(buildVolumeName(vol.getUuid()));

        if (vhost == null) {
            return;
        }

        apiHelper.removeVhostVolumeFromUss(exponVol.getId(), vhost.getId(), uss.getId());
    }

    @Override
    public String getIdentity() {
        return ExponConstants.IDENTITY;
    }

    @Override
    public void connect(String config, String url, ReturnValueCompletion<LinkedHashMap> comp) {
        apiHelper.login();
        ExponAddonInfo info = new ExponAddonInfo();

        List<FailureDomainModule> pools = apiHelper.queryPools();
        pools = pools.stream().map(it -> apiHelper.getPool(it.getId())).collect(Collectors.toList());
        info.setPools(pools.stream().map(ExponAddonInfo.Pool::valueOf).collect(Collectors.toList()));
        List<TianshuClusterModule> clusters = apiHelper.queryClusters();
        info.setClusters(clusters.stream().map(ExponAddonInfo.TianshuCluster::valueOf).collect(Collectors.toList()));
        addonInfo = info;
        comp.success(JSONObjectUtil.rehashObject(addonInfo, LinkedHashMap.class));
    }

    @Override
    public void reportCapacity(ReturnValueCompletion<StorageCapacity> comp) {
        self = dbf.reload(self);
        addonInfo = JSONObjectUtil.toObject(self.getAddonInfo(), ExponAddonInfo.class);

        List<FailureDomainModule> pools = getSelfPools();
        long total = pools.stream().mapToLong(FailureDomainModule::getValidSize).sum();
        long avail = total - pools.stream().mapToLong(FailureDomainModule::getRealDataSize).sum();
        StorageCapacity cap = new StorageCapacity();
        cap.setHealthy(getHealthy(pools));
        cap.setAvailableCapacity(avail);
        cap.setTotalCapacity(total);
        comp.success(cap);
    }

    private StorageHealthy getHealthy(List<FailureDomainModule> pools) {
        if (pools.stream().allMatch(it -> it.getHealthStatus().equals(HealthStatus.health.name()))) {
            return StorageHealthy.Ok;
        } else if (pools.stream().noneMatch(it -> it.getHealthStatus().equals(HealthStatus.health.name()))) {
            return StorageHealthy.Failed;
        } else {
            return StorageHealthy.Warn;
        }
    }

    @Override
    public void reportHealthy(ReturnValueCompletion<StorageHealthy> comp) {
        self = dbf.reload(self);
        addonInfo = JSONObjectUtil.toObject(self.getAddonInfo(), ExponAddonInfo.class);

        List<FailureDomainModule> pools = getSelfPools();
        comp.success(getHealthy(pools));
    }

    private List<FailureDomainModule> getSelfPools() {
        // TODO check config
        Set<String> poolIds = addonInfo.getPools().stream().map(ExponAddonInfo.Pool::getId).collect(Collectors.toSet());

        List<FailureDomainModule> pools = apiHelper.queryPools();
        pools.removeIf(it -> !poolIds.contains(it.getId()));
        pools = pools.stream().map(it -> apiHelper.getPool(it.getId())).collect(Collectors.toList());
        return pools;
    }

    private String getPoolId(String name) {
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
        FailureDomainModule pool = allocateFreePool(aspec.getSize());
        if (pool == null) {
            throw new OperationFailureException(operr("no available pool with enough space[%d] and healthy", aspec.getSize()));
        }

        return buildExponPath(pool.getFailureDomainName(), "");
    }

    private FailureDomainModule allocateFreePool(long size) {
        List<FailureDomainModule> pools = getSelfPools();
        return pools.stream().filter(it -> it.getHealthStatus().equals(HealthStatus.health.name()) &&
                        it.getValidSize() - it.getRealDataSize() > size)
                .max(Comparator.comparingLong(FailureDomainModule::getAvailableCapacity))
                .orElse(null);
    }

    @Override
    public String buildVolumeInstallPath(String volumeUuid, VolumeType volumeType) {
        throw new RuntimeException("not supported");
    }

    @Override
    public String buildVolumeSnapshotInstallPath(String volumeInstallPath, String snapshotUuid) {
        return volumeInstallPath + "@" + snapshotUuid;
    }

    @Override
    public String buildImageInstallPath(String imageUuid, String resourceType) {
        throw new RuntimeException("not supported");
    }

    @Override
    public void createVolume(CreateVolumeSpec v, ReturnValueCompletion<VolumeStats> comp) {
        String poolName;
        v.setSize(Math.max(v.getSize(), MIN_SIZE));

        if (v.getAllocatedUrl() == null) {
            FailureDomainModule pool = allocateFreePool(v.getSize());
            if (pool == null) {
                comp.fail(operr("no available pool with enough space[%d] and healthy", v.getSize()));
                return;
            }
            poolName = pool.getFailureDomainName();
        } else {
            poolName = getPoolNameFromPath(v.getAllocatedUrl());
        }

        String poolId = getPoolId(poolName);

        VolumeModule exponVol = apiHelper.createVolume(v.getName(), poolId, v.getSize());
        VolumeStats stats = new VolumeStats();
        stats.setInstallPath(buildExponPath(poolName, exponVol.getId()));
        stats.setSize(exponVol.getVolumeSize());
        stats.setActualSize(exponVol.getDataSize());
        stats.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
        comp.success(stats);
    }

    @Override
    public void deleteVolume(String installPath, Completion comp) {
        String volId = getVolIdFromPath(installPath);
        apiHelper.deleteVolume(volId);
        comp.success();
    }

    @Override
    public void cloneVolume(String srcInstallPath, CreateVolumeSpec dst, ReturnValueCompletion<VolumeStats> comp) {
        String snapId = getSnapIdFromPath(srcInstallPath);
        VolumeModule vol = apiHelper.cloneVolume(snapId, dst.getName(), VolumeQos.valueOf(dst.getQos()));
        VolumeStats stats = new VolumeStats();
        stats.setInstallPath(buildExponPath(getPoolNameFromPath(srcInstallPath), vol.getId()));
        stats.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
        comp.success(stats);
    }

    @Override
    public void copyVolume(String srcInstallPath, CreateVolumeSpec dst, ReturnValueCompletion<VolumeStats> comp) {
        throw new RuntimeException("not supported");
    }

    @Override
    public void flattenVolume(String installPath, ReturnValueCompletion<VolumeStats> comp) {
        throw new RuntimeException("not supported");
    }

    @Override
    public void stats(String installPath, ReturnValueCompletion<VolumeStats> comp) {
        VolumeModule vol = apiHelper.getVolume(getVolIdFromPath(installPath));
        VolumeStats stats = new VolumeStats();
        stats.setInstallPath(installPath);
        stats.setSize(vol.getVolumeSize());
        stats.setActualSize(vol.getDataSize());
        stats.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
        comp.success(stats);
    }

    @Override
    public void batchStats(Collection<String> installPath, ReturnValueCompletion<List<VolumeStats>> comp) {
        throw new RuntimeException("not supported");
    }

    @Override
    public void expandVolume(String installPath, long size, ReturnValueCompletion<VolumeStats> comp) {
        VolumeModule vol = apiHelper.expandVolume(getVolIdFromPath(installPath), size);
        VolumeStats stats = new VolumeStats();
        stats.setInstallPath(installPath);
        stats.setSize(size);
        comp.success(stats);
    }

    @Override
    public void setVolumeQos(BaseVolumeInfo v, Completion comp) {
        apiHelper.setVolumeQos(getVolIdFromPath(v.getInstallPath()), VolumeQos.valueOf(v.getQos()));
        comp.success();
    }

    @Override
    public <T extends RemoteTarget> void export(ExportSpec espec, Class<T> clz, ReturnValueCompletion<T> comp) {
        if (clz.isAssignableFrom(NvmeRemoteTarget.class)) {
            NvmeRemoteTarget target = exportNvmf(espec);
            comp.success((T) target);
        } else {
            throw new RuntimeException("unsupport target " + clz.getSimpleName());
        }
    }

    NvmeRemoteTarget exportNvmf(ExportSpec espec) {
        String lunId, lunType;
        String source = espec.getInstallPath();
        if (source.contains("@")) {
            lunId = getSnapIdFromPath(source);
            lunType = "snapshot";
        } else {
            lunId = getVolIdFromPath(source);
            lunType = "volume";
        }

        String tianshuId = addonInfo.getClusters().get(0).getId();
        UssGatewayModule uss = getUssGateway(VolumeProtocol.NVMEoF, "zstack");

        String nvmfControllerName = "nvmf_" + lunId;
        NvmfModule nvmf = apiHelper.queryNvmfController(nvmfControllerName);
        if (nvmf == null) {
            nvmf = apiHelper.createNvmfController(nvmfControllerName, tianshuId, lunId);
        }

        String nvmfClientName = "nvmf_" + lunId;
        NvmfClientGroupModule client = apiHelper.queryNvmfClient(nvmfClientName);
        if (client == null) {
            client = apiHelper.createNvmfClient(nvmfClientName, tianshuId, Collections.singletonList(hostNqn));
        }

        // TODO port is hardcode
        apiHelper.bindNvmfTargetToUss(nvmf.getId(), uss.getId(), 4420);
        apiHelper.addNvmfClientToNvmfTarget(client.getId(), nvmf.getId());

        if (lunType.equals("volume")) {
            apiHelper.addVolumeToNvmfClientGroup(lunId, client.getId(), nvmf.getId());
        } else {
            apiHelper.addSnapshotToNvmfClientGroup(lunId, client.getId(), nvmf.getId());
        }

        sleep();

        NvmeRemoteTarget target = new NvmeRemoteTarget();
        target.setHostNqn(hostNqn);
        target.setPort(4420);
        target.setTransport("tcp");
        target.setNqn(nvmf.getNqn());
        target.setIp(uss.getManagerIp());
        target.setDiskId(lunId);
        return target;
    }

    @Override
    public void unexport(String installPath, RemoteTarget target, Completion comp) {
        if (target instanceof NvmeRemoteTarget) {
            unexportNvmf(installPath, (NvmeRemoteTarget) target);
        } else {
            throw new RuntimeException("unsupport target " + target.getClass().getSimpleName());
        }
        comp.success();
    }

    private void unexportNvmf(String source, NvmeRemoteTarget target) {
        String lunId, lunType;
        if (source.contains("@")) {
            lunId = getSnapIdFromPath(source);
            lunType = "snapshot";
        } else {
            lunId = getVolIdFromPath(source);
            lunType = "volume";
        }

        String tianshuId = addonInfo.getClusters().get(0).getId();
        UssGatewayModule uss = getUssGateway(VolumeProtocol.NVMEoF, "zstack");

        String nvmfControllerName = "nvmf_" + lunId;
        NvmfModule nvmf = apiHelper.queryNvmfController(nvmfControllerName);

        String nvmfClientName = "nvmf_" + lunId;
        NvmfClientGroupModule client = apiHelper.queryNvmfClient(nvmfClientName);
        if (nvmf == null && client == null) {
            return;
        }

        if (client == null) {
            apiHelper.deleteNvmfController(nvmf.getId());
            return;
        }

        if (lunType.equals("volume")) {
            apiHelper.removeVolumeFromNvmfClientGroup(lunId, client.getId());
        } else {
            apiHelper.removeSnapshotFromNvmfClientGroup(lunId, client.getId());
        }

        sleep();

        apiHelper.removeNvmfClientFromNvmfTarget(client.getId(), nvmf.getId());
        apiHelper.unbindNvmfTargetToUss(nvmf.getId(), uss.getId());
        apiHelper.deleteNvmfController(nvmf.getId());
        apiHelper.deleteNvmfClient(client.getId());
    }

    private void sleep() {
        // TODO remove it
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void createSnapshot(CreateVolumeSnapshotSpec spec, ReturnValueCompletion<VolumeSnapshotStats> comp) {
        VolumeSnapshotModule snapshot = apiHelper.createVolumeSnapshot(
                getVolIdFromPath(spec.getVolumeInstallPath()), spec.getName(), "todo");
        VolumeSnapshotStats stats = new VolumeSnapshotStats();
        stats.setInstallPath(buildExponSnapshotPath(spec.getVolumeInstallPath(), snapshot.getId()));
        stats.setActualSize(snapshot.getDataSize());
        comp.success(stats);
    }

    @Override
    public void deleteSnapshot(String installPath, Completion comp) {
        apiHelper.deleteVolumeSnapshot(getSnapIdFromPath(installPath));
        comp.success();
    }

    @Override
    public void revertVolumeSnapshot(String snapshotInstallPath, ReturnValueCompletion<VolumeStats> comp) {
        String volId = getVolIdFromPath(snapshotInstallPath);
        String snapId = getSnapIdFromPath(snapshotInstallPath);
        apiHelper.recoverySnapshot(volId, snapId);
    }

    @Override
    public void validateConfig(String config) {

    }
}
