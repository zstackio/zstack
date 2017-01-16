package org.zstack.header.identity

import org.zstack.header.errorcode.ErrorCode

doc {

    title "用户组清单"

    ref {
        name "error"
        path "org.zstack.header.identity.APICreateUserGroupEvent.error"
        desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
        type "ErrorCode"
        since "0.6"
        clz ErrorCode.class
    }
    ref {
        name "inventory"
        path "org.zstack.header.identity.APICreateUserGroupEvent.inventory"
        desc "用户组清单"
        type "UserGroupInventory"
        since "0.6"
        clz UserGroupInventory.class
    }
}
