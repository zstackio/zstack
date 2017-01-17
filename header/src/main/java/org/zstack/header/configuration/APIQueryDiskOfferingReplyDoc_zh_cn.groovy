package org.zstack.header.configuration

import org.zstack.header.errorcode.ErrorCode

doc {

    title "云盘规格清单列表"

    ref {
        name "error"
        path "org.zstack.header.configuration.APIQueryDiskOfferingReply.error"
        desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
        type "ErrorCode"
        since "0.6"
        clz ErrorCode.class
    }
    ref {
        name "inventories"
        path "org.zstack.header.configuration.APIQueryDiskOfferingReply.inventories"
        desc "云盘规格清单列表"
        type "List"
        since "0.6"
        clz DiskOfferingInventory.class
    }
}
