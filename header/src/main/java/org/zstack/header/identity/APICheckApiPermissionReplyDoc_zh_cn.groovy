package org.zstack.header.identity

import org.zstack.header.errorcode.ErrorCode

doc {

    title "API权限清单"

    ref {
        name "error"
        path "org.zstack.header.identity.APICheckApiPermissionReply.error"
        desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
        type "ErrorCode"
        since "0.6"
        clz ErrorCode.class
    }
    field {
        name "inventory"
        desc "API权限清单"
        type "Map"
        since "0.6"
    }
}
