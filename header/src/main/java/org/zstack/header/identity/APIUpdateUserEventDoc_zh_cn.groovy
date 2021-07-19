package org.zstack.header.identity

import org.zstack.header.errorcode.ErrorCode

doc {

    title "用户清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
    ref {
        name "error"
        path "org.zstack.header.identity.APIUpdateUserEvent.error"
        desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
        type "ErrorCode"
        since "0.6"
        clz ErrorCode.class
    }
    ref {
        name "inventory"
        path "org.zstack.header.identity.APIUpdateUserEvent.inventory"
        desc "用户清单"
        type "UserInventory"
        since "0.6"
        clz UserInventory.class
    }
}
