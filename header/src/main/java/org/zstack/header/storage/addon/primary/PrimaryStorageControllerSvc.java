package org.zstack.header.storage.addon.primary;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.storage.addon.RemoteTarget;
import org.zstack.header.storage.addon.StorageCapacity;
import org.zstack.header.storage.addon.StorageHealthy;
import org.zstack.header.storage.snapshot.VolumeSnapshotStats;
import org.zstack.header.volume.VolumeStats;
import org.zstack.header.volume.VolumeType;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

public interface PrimaryStorageControllerSvc {
    String getIdentity();
    void connect(String config, String url, ReturnValueCompletion<LinkedHashMap> comp);

    void reportCapacity(ReturnValueCompletion<StorageCapacity> comp);
    void reportHealthy(ReturnValueCompletion<StorageHealthy> comp);
    StorageCapabilities reportCapabilities();

    String allocateSpace(AllocateSpaceSpec aspec);
    String buildVolumeInstallPath(String volumeUuid, VolumeType volumeType);

    String buildVolumeSnapshotInstallPath(String volumeInstallPath, String snapshotUuid);
    String buildImageInstallPath(String imageUuid, String resourceType);

    void createVolume(CreateVolumeSpec v, ReturnValueCompletion<VolumeStats>comp);
    void deleteVolume(String installPath, Completion comp);
    void cloneVolume(String srcInstallPath, CreateVolumeSpec dst, ReturnValueCompletion<VolumeStats>comp);
    void copyVolume(String srcInstallPath, CreateVolumeSpec dst, ReturnValueCompletion<VolumeStats>comp);
    void flattenVolume(String installPath, ReturnValueCompletion<VolumeStats>comp);

    void stats(String installPath, ReturnValueCompletion<VolumeStats> comp);

    void batchStats(Collection<String> installPath, ReturnValueCompletion<List<VolumeStats>> comp);

    void expandVolume(String installPath, long size, ReturnValueCompletion<VolumeStats> comp);
    void setVolumeQos(BaseVolumeInfo v, Completion comp);

    <T extends RemoteTarget> void export(ExportSpec espec, Class<T> clz, ReturnValueCompletion<T> comp);
    void unexport(String installPath, RemoteTarget target, Completion comp);

    void createSnapshot(CreateVolumeSnapshotSpec spec, ReturnValueCompletion<VolumeSnapshotStats> comp);
    void deleteSnapshot(String installPath, Completion comp);
    void revertVolumeSnapshot(String snapshotInstallPath, ReturnValueCompletion<VolumeStats> comp);

    void validateConfig(String config);
}
