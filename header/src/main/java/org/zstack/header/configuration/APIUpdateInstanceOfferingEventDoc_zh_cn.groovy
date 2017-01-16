package org.zstack.header.configuration

import org.zstack.header.errorcode.ErrorCode

doc {

    title "云主机规格清单"

    ref {
        name "error"
        path "org.zstack.header.configuration.APIUpdateInstanceOfferingEvent.error"
        desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
        type "ErrorCode"
        since "0.6"
        clz ErrorCode.class
    }
    ref {
        name "inventory"
        path "org.zstack.header.configuration.APIUpdateInstanceOfferingEvent.inventory"
        desc "云主机规格清单"
        type "InstanceOfferingInventory"
        since "0.6"
        clz InstanceOfferingInventory.class
    }
}
