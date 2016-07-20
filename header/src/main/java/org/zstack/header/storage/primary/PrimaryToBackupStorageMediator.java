package org.zstack.header.storage.primary;

import org.zstack.header.host.HypervisorType;
import org.zstack.header.storage.backup.BackupStorageType;

import java.util.List;

public interface PrimaryToBackupStorageMediator {
    String getSupportedPrimaryStorageType();
    
    String getSupportedBackupStorageType();
    
    List<String> getSupportedHypervisorTypes();
}
