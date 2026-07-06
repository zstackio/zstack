package org.zstack.storage.encrypt;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.Q;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO_;
import org.zstack.header.storage.primary.PrimaryStorageFeature;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vm.DiskAO;
import org.zstack.header.vm.VmAllocatePrimaryStorageExtensionPoint;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.VolumeProtocol;
import org.zstack.storage.primary.PrimaryStorageFeatureAllocatorExtensionPoint;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EncryptedVolumePrimaryStorageAllocatorExtension implements VmAllocatePrimaryStorageExtensionPoint,
        PrimaryStorageFeatureAllocatorExtensionPoint {
    private static final String ZHPS_PRIMARY_STORAGE_IDENTITY = "expon";
    private static final String ZHPS_PRIMARY_STORAGE_PROTOCOL = VolumeProtocol.Vhost.name();
    @Autowired
    private VolumeSourceEncryptionResolver sourceEncryptionResolver;

    @Override
    public List<PrimaryStorageVO> allocatePrimaryStorage(Set<PrimaryStorageFeature> requiredFeatures,
                                                         List<PrimaryStorageVO> candidates) {
        if (requiredFeatures != null && candidates != null
                && requiredFeatures.contains(PrimaryStorageFeature.ENCRYPTED_VOLUME)
                && !candidates.isEmpty()) {
            filterEncryptedVolumeUnsupportedPrimaryStorage(candidates);
        }
        return candidates;
    }

    @Override
    public void filterPrimaryStorageCandidates(VmInstanceSpec spec, List<String> rootPrimaryStorageUuids,
                                               boolean rootPrimaryStorageAutoAllocation,
                                               List<String> dataPrimaryStorageUuids,
                                               boolean dataPrimaryStorageAutoAllocation) {
        sourceEncryptionResolver.resolve(spec);
        if (requiresEncryptedRootVolumeAutoPsFilter(spec, rootPrimaryStorageUuids, rootPrimaryStorageAutoAllocation)) {
            filterEncryptedVolumeUnsupportedPrimaryStorageUuids(rootPrimaryStorageUuids);
        }
        if (requiresEncryptedDataVolumeAutoPsFilter(spec, dataPrimaryStorageUuids, dataPrimaryStorageAutoAllocation)) {
            filterEncryptedVolumeUnsupportedPrimaryStorageUuids(dataPrimaryStorageUuids);
        }
    }

    private void filterEncryptedVolumeUnsupportedPrimaryStorage(List<PrimaryStorageVO> candidates) {
        List<String> psUuids = candidates.stream().map(PrimaryStorageVO::getUuid).collect(Collectors.toList());
        Set<String> unsupportedPsUuids = new HashSet<>(getEncryptedVolumeUnsupportedPrimaryStorageUuids(psUuids));
        candidates.removeIf(ps -> unsupportedPsUuids.contains(ps.getUuid()));
    }

    private boolean requiresEncryptedRootVolumeAutoPsFilter(VmInstanceSpec spec, List<String> candidates,
                                                           boolean autoAllocation) {
        return autoAllocation && spec != null && isEncrypted(spec.getRootDisk())
                && candidates != null && !candidates.isEmpty();
    }

    private boolean requiresEncryptedDataVolumeAutoPsFilter(VmInstanceSpec spec, List<String> candidates,
                                                           boolean autoAllocation) {
        return autoAllocation && spec != null
                && spec.getNonTemplateDeprecatedDisksSpecs().stream().anyMatch(this::isEncrypted)
                && candidates != null && !candidates.isEmpty();
    }

    private void filterEncryptedVolumeUnsupportedPrimaryStorageUuids(List<String> candidates) {
        Set<String> unsupportedPsUuids = new HashSet<>(getEncryptedVolumeUnsupportedPrimaryStorageUuids(candidates));
        candidates.removeIf(unsupportedPsUuids::contains);
    }

    private boolean isEncrypted(DiskAO disk) {
        return disk != null && Boolean.TRUE.equals(disk.getEncrypted());
    }

    protected List<String> getEncryptedVolumeUnsupportedPrimaryStorageUuids(List<String> psUuids) {
        Q query = Q.New(ExternalPrimaryStorageVO.class)
                .select(ExternalPrimaryStorageVO_.uuid)
                .eq(ExternalPrimaryStorageVO_.identity, ZHPS_PRIMARY_STORAGE_IDENTITY)
                .eq(ExternalPrimaryStorageVO_.defaultProtocol, ZHPS_PRIMARY_STORAGE_PROTOCOL);
        if (psUuids != null) {
            query.in(ExternalPrimaryStorageVO_.uuid, psUuids);
        }
        return query.listValues();
    }
}
