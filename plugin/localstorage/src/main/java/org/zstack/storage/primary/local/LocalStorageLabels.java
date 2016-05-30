package org.zstack.storage.primary.local;

import org.zstack.core.logging.LogLabel;

/**
 * Created by xing5 on 2016/6/3.
 */
public class LocalStorageLabels {
    @LogLabel(messages = {
            "en_US = initialize local primary storage on the host",
            "zh_CN = 初始化本地主存储"
    })
    public static final String INIT = "localStorage.kvm.init";
}
