package org.zstack.header.vm.additions;

import org.zstack.header.core.Completion;

public interface VmHostFileManager {
    void backupVmHostFiles(String vmUuid, String resourceUuid, Completion completion);
    void cleanVmHostBackupFile(String resourceUuid);
}
