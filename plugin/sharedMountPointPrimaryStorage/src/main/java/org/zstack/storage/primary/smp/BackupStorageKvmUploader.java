package org.zstack.storage.primary.smp;

import org.zstack.header.core.Completion;

/**
 * Created by xing5 on 2016/3/27.
 */
public abstract class BackupStorageKvmUploader {
    public abstract void uploadBits(String bsPath, String psPath, Completion completion);
}
