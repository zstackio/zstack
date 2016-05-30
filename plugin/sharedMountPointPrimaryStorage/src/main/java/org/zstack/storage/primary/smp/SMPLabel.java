package org.zstack.storage.primary.smp;

import org.zstack.core.logging.LogLabel;

/**
 * Created by xing5 on 2016/6/3.
 */
public class SMPLabel {
    @LogLabel(messages = {
            "en_US = initialize Shared Mount Point primary storage[UUID: {0}] on the host",
            "zh_CN = 初始化Shared Mount Point[UUID: {0}]主存储"
    })
    public static final String INIT = "smpPrimaryStorage.init";
}
