package org.zstack.header.identity

import org.zstack.header.errorcode.ErrorCode

doc {

    title "会话清单"

    ref {
        name "error"
        path "org.zstack.header.identity.APILogInReply.error"
        desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
        type "ErrorCode"
        since "0.6"
        clz ErrorCode.class
    }
    ref {
        name "inventory"
        path "org.zstack.header.identity.APILogInReply.inventory"
        desc "会话清单"
        type "SessionInventory"
        since "0.6"
        clz SessionInventory.class
    }
}
