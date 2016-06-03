package org.zstack.storage.ceph;

import org.zstack.core.logging.LogLabel;

/**
 * Created by xing5 on 2016/6/3.
 */
public class CephLabel {
    @LogLabel(messages = {
            "en_US = create CEPH secret on the host for the primary storage[UUID: {0}]",
            "zh_CN = 创建访问CEPH主存储[UUID: {0}]需要的Secret信息"
    })
    public static final String PS_CREATE_SECRET = "ceph.ps.createSecret";
}
