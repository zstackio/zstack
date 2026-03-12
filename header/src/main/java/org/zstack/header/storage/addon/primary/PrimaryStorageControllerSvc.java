package org.zstack.header.storage.addon.primary;

import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.host.HostInventory;
import org.zstack.header.storage.addon.NodeHealthy;
import org.zstack.header.storage.addon.RemoteTarget;
import org.zstack.header.storage.addon.StorageCapacity;
import org.zstack.header.storage.addon.StorageHealthy;
import org.zstack.header.storage.snapshot.VolumeSnapshotStats;
import org.zstack.header.volume.VolumeProtocol;
import org.zstack.header.volume.VolumeStats;

import java.util.Collection;
import java.util.List;

public interface PrimaryStorageControllerSvc {
    String getIdentity();

    /**
     * connect to storage system, and the AddonInfo may be changed after connection,
     * report the latest AddonInfo via the completion, ZStack will persist it on all management nodes.
     * @param config
     * @param url
     * @param comp
     */
    void connect(String config, String url, ReturnValueCompletion<AddonInfo> comp);

    /**
     * ping the storage system, and report the latest AddonInfo and result via the completion,
     * ZStack will persist AddonInfo on all management nodes.
     * @param completion
     */
    void ping(ReturnValueCompletion<PingResult> completion);

    /**
     * sync the addon info from other management node to this svc
     * @param addonInfo
     */
    void syncAddonInfo(String addonInfo);

    /**
     * sync the config from other management node to this svc
     * @param config
     */
    void syncConfig(String config);

    void getCapacity(List<String> requiredUrls, ReturnValueCompletion<StorageCapacity> comp);
    void reportCapacity(ReturnValueCompletion<StorageCapacity> comp);
    void reportHealthy(ReturnValueCompletion<StorageHealthy> comp);
    void reportNodeHealthy(HostInventory host, ReturnValueCompletion<NodeHealthy> comp);
    StorageCapabilities reportCapabilities();

    // TODO: remove this method in future
    @Deprecated
    String allocateSpace(AllocateSpaceSpec aspec);

    void createVolume(CreateVolumeSpec v, ReturnValueCompletion<VolumeStats>comp);
    void deleteVolume(String installPath, Completion comp);
    void deleteVolumeAndSnapshot(String installPath, Completion comp);
    void trashVolume(String installPath, Completion comp);
    void cloneVolume(String srcInstallPath, CreateVolumeSpec dst, ReturnValueCompletion<VolumeStats>comp);
    void copyVolume(String srcInstallPath, CreateVolumeSpec dst, ReturnValueCompletion<VolumeStats>comp);
    void flattenVolume(String installPath, ReturnValueCompletion<VolumeStats>comp);

    // support uri or path
    void stats(String installPath, ReturnValueCompletion<VolumeStats> comp);

    void batchStats(Collection<String> installPath, ReturnValueCompletion<List<VolumeStats>> comp);

    void expandVolume(String installPath, long size, ReturnValueCompletion<VolumeStats> comp);
    void setVolumeQos(BaseVolumeInfo v, Completion comp);
    void deleteVolumeQos(BaseVolumeInfo v, Completion comp);
    void export(ExportSpec espec, VolumeProtocol protocol, ReturnValueCompletion<RemoteTarget> comp);
    void unexport(ExportSpec espec, RemoteTarget remoteTarget, VolumeProtocol protocol, Completion comp);

    void createSnapshot(CreateVolumeSnapshotSpec spec, ReturnValueCompletion<VolumeSnapshotStats> comp);
    void deleteSnapshot(String installPath, Completion comp);
    void expungeSnapshot(String installPath, Completion comp);
    void revertVolumeSnapshot(String snapshotInstallPath, ReturnValueCompletion<VolumeStats> comp);

    String validateConfig(String config) throws ApiMessageInterceptionException;

    void setTrashExpireTime(int timeInSeconds, Completion completion);
    void onFirstAdditionConfigure(Completion completion);

    long alignSize(long size);
}
