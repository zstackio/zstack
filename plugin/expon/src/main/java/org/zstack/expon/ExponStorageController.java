package org.zstack.expon;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.expon.sdk.ExponClient;
import org.zstack.expon.sdk.ExponConfig;
import org.zstack.expon.sdk.cluster.TianshuClusterModule;
import org.zstack.expon.sdk.iscsi.IscsiClientGroupModule;
import org.zstack.expon.sdk.iscsi.IscsiModule;
import org.zstack.expon.sdk.iscsi.IscsiUssResource;
import org.zstack.expon.sdk.nvmf.NvmfBoundUssGatewayRefModule;
import org.zstack.expon.sdk.nvmf.NvmfClientGroupModule;
import org.zstack.expon.sdk.nvmf.NvmfModule;
import org.zstack.expon.sdk.pool.FailureDomainModule;
import org.zstack.expon.sdk.uss.UssGatewayModule;
import org.zstack.expon.sdk.vhost.VHostControllerModule;
import org.zstack.expon.sdk.volume.VolumeModule;
import org.zstack.expon.sdk.volume.ExponVolumeQos;
import org.zstack.expon.sdk.volume.VolumeSnapshotModule;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.expon.HealthStatus;
import org.zstack.header.host.HostInventory;
import org.zstack.header.storage.addon.*;
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
import static org.zstack.storage.addon.primary.ExternalPrimaryStorageNameHelper.buildImageName;
import static org.zstack.storage.addon.primary.ExternalPrimaryStorageNameHelper.buildVolumeName;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ExponStorageController implements PrimaryStorageControllerSvc, PrimaryStorageNodeSvc {

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
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
        scap.setSupportCreateOnHypervisor(false);
        capabilities.setSnapshotCapability(scap);
        capabilities.setSupportCloneFromVolume(false);
        capabilities.setSupportStorageQos(true);
        capabilities.setSupportLiveExpandVolume(false);
        capabilities.setSupportedImageFormats(Collections.singletonList("raw"));
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

    private VHostControllerModule getOrCreateVhostController(String name) {
        VHostControllerModule vhost = apiHelper.getVhostController(name);
        if (vhost == null) {
            vhost = apiHelper.createVHostController(name);
        }

        return vhost;
    }

    @Override
    public void activate(BaseVolumeInfo v, HostInventory h, boolean shareable, ReturnValueCompletion<ActiveVolumeTO> comp) {
        ActiveVolumeTO to;
        if (VolumeProtocol.VHost.toString().equals(v.getProtocol())) {
            to = activeVhostVolume(h, v);
            comp.success(to);
            return;
        } else if (VolumeProtocol.iSCSI.toString().equals(v.getProtocol())) {
            to = activeIscsiVolume(h, v);
            comp.success(to);
            return;
        }

        comp.fail(operr("not supported protocol[%s]", v.getProtocol()));
    }


    private ActiveVolumeTO activeVhostVolume(HostInventory h, BaseVolumeInfo vol) {
        String vhostName = buildVhostControllerName(vol.getUuid());

        UssGatewayModule uss = getUssGateway(VolumeProtocol.VHost, h.getManagementIp());
        VolumeModule exponVol = getVolumeModule(vol);
        if (exponVol == null) {
            throw new RuntimeException("volume not found");
        }

        VHostControllerModule vhost = getOrCreateVhostController(vhostName);
        apiHelper.addVhostVolumeToUss(exponVol.getId(), vhost.getId(), uss.getId());
        VHostVolumeTO to = new VHostVolumeTO();
        to.setInstallPath(vhost.getPath());
        return to;
    }

    private ActiveVolumeTO activeIscsiVolume(HostInventory h, BaseVolumeInfo vol) {
        // TODO
        return null;
    }

    @Override
    public ActiveVolumeTO getActiveResult(BaseVolumeInfo v, boolean shareable) {
        if (VolumeProtocol.VHost.toString().equals(v.getProtocol())) {
            String vhostName = buildVhostControllerName(v.getUuid());
            VHostControllerModule vhost = getOrCreateVhostController(vhostName);
            VHostVolumeTO to = new VHostVolumeTO();
            to.setInstallPath(vhost.getPath());
            return to;
        } else if (VolumeProtocol.iSCSI.toString().equals(v.getProtocol())) {
            // TODO
        }

        throw new OperationFailureException(operr("not supported protocol[%s]", v.getProtocol()));
    }

    @Override
    public void deactivate(BaseVolumeInfo vol, HostInventory h, Completion comp) {
        if (VolumeProtocol.VHost.toString().equals(vol.getProtocol())) {
            deactiveVhost(vol, h);
            comp.success();
            return;
        } else if (VolumeProtocol.iSCSI.toString().equals(vol.getProtocol())) {
            deactiveIscsi(vol, h);
            comp.success();
            return;
        }

        comp.fail(operr("not supported protocol[%s]", vol.getProtocol()));
    }

    private void deactiveVhost(BaseVolumeInfo vol, HostInventory h) {
        String vhostName = buildVhostControllerName(vol.getUuid());

        UssGatewayModule uss = getUssGateway(VolumeProtocol.VHost, h.getManagementIp());
        VHostControllerModule vhost = apiHelper.getVhostController(vhostName);
        if (vhost == null) {
            return;
        }

        VolumeModule exponVol = getVolumeModule(vol);
        apiHelper.removeVhostVolumeFromUss(exponVol.getId(), vhost.getId(), uss.getId());
    }

    private void deactiveIscsi(BaseVolumeInfo vol, HostInventory h) {

    }


    private VolumeModule getVolumeModule(BaseVolumeInfo vol) {
        if ("image".equals(vol.getType())) {
            return apiHelper.queryVolume(buildImageName(vol.getUuid()));
        } else {
            return apiHelper.queryVolume(buildVolumeName(vol.getUuid()));
        }
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
        VolumeModule vol = apiHelper.cloneVolume(snapId, dst.getName(), ExponVolumeQos.valueOf(dst.getQos()));
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
        apiHelper.setVolumeQos(getVolIdFromPath(v.getInstallPath()), ExponVolumeQos.valueOf(v.getQos()));
        comp.success();
    }

    @Override
    public void export(ExportSpec espec, VolumeProtocol protocol, ReturnValueCompletion<RemoteTarget> comp) {
        if (protocol == VolumeProtocol.NVMEoF) {
            NvmeRemoteTarget target = exportNvmf(espec);
            comp.success(target);
        } else if (protocol == VolumeProtocol.iSCSI) {
            IscsiRemoteTarget target = exportIscsi(espec);
            comp.success(target);
        } else {
            throw new RuntimeException("unsupport target " + protocol.name());
        }
    }

    synchronized NvmeRemoteTarget exportNvmf(ExportSpec espec) {
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

        String nvmfControllerName = "nvmf_zstack";
        NvmfModule nvmf = apiHelper.queryNvmfController(nvmfControllerName);
        if (nvmf == null) {
            nvmf = apiHelper.createNvmfController(nvmfControllerName, tianshuId, lunId);
        }

        NvmfBoundUssGatewayRefModule ref = apiHelper.getNvmfBoundUssGateway(nvmf.getId(), uss.getId());
        if (ref == null) {
            ref = apiHelper.bindNvmfTargetToUss(nvmf.getId(), uss.getId(), 4420);
        }

        String nvmfClientName = "nvmf_zstack";
        NvmfClientGroupModule client = apiHelper.queryNvmfClient(nvmfClientName);
        if (client == null) {
            client = apiHelper.createNvmfClient(nvmfClientName, tianshuId, Collections.singletonList(hostNqn));
            apiHelper.addNvmfClientToNvmfTarget(client.getId(), nvmf.getId());
        }

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
        target.setIp(ref.getBindIp());
        target.setDiskId(lunId);
        return target;
    }

    synchronized IscsiRemoteTarget exportIscsi(ExportSpec espec) {
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
        UssGatewayModule uss = getUssGateway(VolumeProtocol.iSCSI, "zstack");

        String iscsiControllerName = "iscsi_zstack";
        IscsiModule iscsi = apiHelper.queryIscsiController(iscsiControllerName);
        if (iscsi == null) {
            IscsiUssResource ussRes = new IscsiUssResource();
            ussRes.setServerId(uss.getServerId());
            ussRes.setGatewayIp(uss.getBusinessNetwork().split("/")[0]);
            iscsi = apiHelper.createIscsiController(iscsiControllerName, tianshuId, 3260, ussRes);
        }

        String iscsiClientName = getIscsiClientName(espec.getClientIp());
        IscsiClientGroupModule client = apiHelper.queryIscsiClient(iscsiClientName);
        if (client == null) {
            client = apiHelper.createIscsiClient(iscsiClientName, tianshuId, Collections.singletonList(espec.getClientIp()));
            apiHelper.addIscsiClientToIscsiTarget(client.getId(), iscsi.getId());
            sleep();
        }

        if (lunType.equals("volume")) {
            apiHelper.addVolumeToIscsiClientGroup(lunId, client.getId(), iscsi.getId());
        } else {
            apiHelper.addSnapshotToIscsiClientGroup(lunId, client.getId(), iscsi.getId());
        }

        sleep();

        IscsiRemoteTarget target = new IscsiRemoteTarget();
        target.setPort(3260);
        target.setTransport("tcp");
        target.setIqn(iscsi.getIqn());
        target.setIp(uss.getBusinessNetwork().split("/")[0]);
        target.setDiskId(lunId.replace("-", "").substring(16, 32));
        target.setClientIp(espec.getClientIp());
        return target;
    }

    private String getIscsiClientName(String clientIp) {
        return "iscsi_" + clientIp.replace(".", "_");
    }

    @Override
    public void unexport(String installPath, RemoteTarget target, Completion comp) {
        if (target instanceof NvmeRemoteTarget) {
            unexportNvmf(installPath, (NvmeRemoteTarget) target);
        } else if (target instanceof IscsiRemoteTarget) {
            unexportIscsi(installPath, (IscsiRemoteTarget) target);
        } else {
            throw new RuntimeException("unsupport target " + target.getClass().getSimpleName());
        }
        comp.success();
    }

    private synchronized void unexportNvmf(String source, NvmeRemoteTarget target) {
        String lunId, lunType;
        if (source.contains("@")) {
            lunId = getSnapIdFromPath(source);
            lunType = "snapshot";
        } else {
            lunId = getVolIdFromPath(source);
            lunType = "volume";
        }

        // UssGatewayModule uss = getUssGateway(VolumeProtocol.NVMEoF, "zstack");

        String nvmfControllerName = "nvmf_zstack";
        NvmfModule nvmf = apiHelper.queryNvmfController(nvmfControllerName);

        String nvmfClientName = "nvmf_zstack";
        NvmfClientGroupModule client = apiHelper.queryNvmfClient(nvmfClientName);
        if (nvmf == null || client == null) {
            return;
        }

        if (lunType.equals("volume")) {
            apiHelper.removeVolumeFromNvmfClientGroup(lunId, client.getId());
        } else {
            apiHelper.removeSnapshotFromNvmfClientGroup(lunId, client.getId());
        }

        sleep();

        /*
        apiHelper.removeNvmfClientFromNvmfTarget(client.getId(), nvmf.getId());
        apiHelper.unbindNvmfTargetToUss(nvmf.getId(), uss.getId());
        apiHelper.deleteNvmfController(nvmf.getId());
        apiHelper.deleteNvmfClient(client.getId());
         */
    }

    private synchronized void unexportIscsi(String source, IscsiRemoteTarget target) {
        String lunId, lunType;
        if (source.contains("@")) {
            lunId = getSnapIdFromPath(source);
            lunType = "snapshot";
        } else {
            lunId = getVolIdFromPath(source);
            lunType = "volume";
        }

        // UssGatewayModule uss = getUssGateway(VolumeProtocol.iSCSI, "zstack");

        String iscsiControllerName = "iscsi_zstack";
        IscsiModule iscsi = apiHelper.queryIscsiController(iscsiControllerName);

        String iscsiClientName = getIscsiClientName(target.getClientIp());
        IscsiClientGroupModule client = apiHelper.queryIscsiClient(iscsiClientName);
        if (iscsi == null || client == null) {
            return;
        }

        if (lunType.equals("volume")) {
            apiHelper.removeVolumeFromIscsiClientGroup(lunId, client.getId());
        } else {
            apiHelper.removeSnapshotFromIscsiClientGroup(lunId, client.getId());
        }

        sleep();

        /*
        apiHelper.removeIscsiClientFromIscsiTarget(client.getId(), iscsi.getId());
        apiHelper.unbindIscsiTargetToUss(iscsi.getId(), uss.getId());
        apiHelper.deleteIscsiController(iscsi.getId());
        apiHelper.deleteIscsiClient(client.getId());
         */
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
        String poolName = getPoolNameFromPath(snapshotInstallPath);
        VolumeModule vol = apiHelper.recoverySnapshot(volId, snapId);

        VolumeStats stats = new VolumeStats();
        stats.setInstallPath(buildExponPath(poolName, volId));
        stats.setSize(vol.getVolumeSize());
        stats.setActualSize(vol.getDataSize());
        stats.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
        comp.success(stats);
    }

    @Override
    public void validateConfig(String config) {

    }
}
