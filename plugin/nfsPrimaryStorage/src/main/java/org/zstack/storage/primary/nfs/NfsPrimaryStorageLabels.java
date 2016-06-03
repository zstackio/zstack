package org.zstack.storage.primary.nfs;

import org.zstack.core.logging.LogLabel;

/**
 * Created by xing5 on 2016/6/3.
 */
public class NfsPrimaryStorageLabels {
    @LogLabel(messages = {
            "en_US = initialize NFS primary storage on the host",
            "zh_CN = 初始化NFS共享主存储"
    })
    public static final String INIT = "nfs.kvm.init";
}
