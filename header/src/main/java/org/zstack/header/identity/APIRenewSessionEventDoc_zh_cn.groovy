package org.zstack.header.identity

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.identity.SessionInventory

doc {

	title "会话清单"

	ref {
		name "error"
		path "org.zstack.header.identity.APIRenewSessionEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.3"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.identity.APIRenewSessionEvent.inventory"
		desc "null"
		type "SessionInventory"
		since "2.3"
		clz SessionInventory.class
	}
}
