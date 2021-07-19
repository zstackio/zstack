package org.zstack.header.identity

import org.zstack.header.errorcode.ErrorCode

doc {

    title "账户清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
    ref {
        name "error"
        path "org.zstack.header.identity.APIUpdateAccountEvent.error"
        desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
        type "ErrorCode"
        since "0.6"
        clz ErrorCode.class
    }
    ref {
        name "inventory"
        path "org.zstack.header.identity.APIUpdateAccountEvent.inventory"
        desc "账户清单"
        type "AccountInventory"
        since "0.6"
        clz AccountInventory.class
    }
}
