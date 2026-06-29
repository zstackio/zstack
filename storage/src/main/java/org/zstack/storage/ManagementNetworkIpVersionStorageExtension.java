package org.zstack.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.Q;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.header.storage.backup.BackupStorageZoneRefVO;
import org.zstack.header.storage.backup.BackupStorageZoneRefVO_;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.storage.primary.PrimaryStorageConstants;
import org.zstack.header.zone.ManagementNetworkIpVersionManager;
import org.zstack.header.zone.ManagementNetworkIpVersionResourceExtensionPoint;

import java.util.List;

import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_STORAGE_BACKUP_10135;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_STORAGE_PRIMARY_10056;

public class ManagementNetworkIpVersionStorageExtension implements ManagementNetworkIpVersionResourceExtensionPoint {
    private static final String PRIMARY_STORAGE_RESOURCE_TYPE = "primary storage";
    private static final String BACKUP_STORAGE_RESOURCE_TYPE = "backup storage";

    @Autowired
    private ManagementNetworkIpVersionManager managementNetworkIpVersionManager;

    @Override
    public void validateExistingResourcesInZone(String zoneUuid, String ipVersion) {
        validatePrimaryStorages(zoneUuid, ipVersion);
        validateBackupStorages(zoneUuid, ipVersion);
    }

    private void validatePrimaryStorages(String zoneUuid, String ipVersion) {
        List<PrimaryStorageVO> primaryStorages = Q.New(PrimaryStorageVO.class)
                .eq(PrimaryStorageVO_.zoneUuid, zoneUuid)
                .notEq(PrimaryStorageVO_.type, PrimaryStorageConstants.LOCAL_STORAGE_TYPE)
                .list();

        for (PrimaryStorageVO ps : primaryStorages) {
            managementNetworkIpVersionManager.validateEndpointMatchesIpVersion(zoneUuid, ipVersion, ps.getUrl(),
                    PRIMARY_STORAGE_RESOURCE_TYPE, ps.getUuid(), ORG_ZSTACK_STORAGE_PRIMARY_10056);
        }
    }

    private void validateBackupStorages(String zoneUuid, String ipVersion) {
        List<String> backupStorageUuids = Q.New(BackupStorageZoneRefVO.class)
                .select(BackupStorageZoneRefVO_.backupStorageUuid)
                .eq(BackupStorageZoneRefVO_.zoneUuid, zoneUuid)
                .listValues();
        if (backupStorageUuids.isEmpty()) {
            return;
        }

        List<BackupStorageVO> backupStorages = Q.New(BackupStorageVO.class)
                .in(BackupStorageVO_.uuid, backupStorageUuids)
                .list();

        for (BackupStorageVO bs : backupStorages) {
            managementNetworkIpVersionManager.validateEndpointMatchesIpVersion(zoneUuid, ipVersion, bs.getUrl(),
                    BACKUP_STORAGE_RESOURCE_TYPE, bs.getUuid(), ORG_ZSTACK_STORAGE_BACKUP_10135);
        }
    }
}
