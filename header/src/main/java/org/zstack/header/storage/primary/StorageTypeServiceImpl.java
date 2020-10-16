package org.zstack.header.storage.primary;

import org.zstack.header.storage.backup.BackupStorageType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Wenhao.Zhang on 20/10/16
 */
public class StorageTypeServiceImpl implements StorageTypeService {
    
    Map<String, List<String>> backupStoragePrimaryStorageMetrics;
    
    @Override
    public void setBackupStoragePrimaryStorageMetrics(Map<String, List<String>> metrics) {
        metrics.forEach((key, value) -> {
            final BackupStorageType bsType = BackupStorageType.createIfAbsent(key);
        
            List<PrimaryStorageType> psTypes = value.stream().map(PrimaryStorageType::createIfAbsent)
                .collect(Collectors.toList());
            bsType.setRelatedPrimaryStorageTypes(psTypes);
        
            psTypes.forEach(psType -> {
                List<BackupStorageType> bsTypes = psType.getRelatedBackupStorageTypes();
                if (bsTypes != null) {
                    bsTypes.add(bsType);
                } else {
                    bsTypes = new ArrayList<>();
                    bsTypes.add(bsType);
                    psType.setRelatedBackupStorageTypes(bsTypes);
                }
            });
        });
        
        this.backupStoragePrimaryStorageMetrics = metrics;
    }
    
    @Override
    public Map<String, List<String>> getBackupStoragePrimaryStorageMetrics() {
        return backupStoragePrimaryStorageMetrics;
    }
}
