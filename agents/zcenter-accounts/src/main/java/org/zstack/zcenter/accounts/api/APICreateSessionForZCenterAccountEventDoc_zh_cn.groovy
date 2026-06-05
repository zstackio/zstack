package org.zstack.zcenter.accounts.api

import org.zstack.header.identity.SessionInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "为 ZCenter 账户创建会话的结果"

	ref {
		name "inventory"
		path "org.zstack.zcenter.accounts.api.APICreateSessionForZCenterAccountEvent.inventory"
		desc "会话"
		type "SessionInventory"
		since "5.1.0"
		clz SessionInventory.class
	}
	field {
		name "success"
		desc "创建是否成功"
		type "boolean"
		since "5.1.0"
	}
	ref {
		name "error"
		path "org.zstack.zcenter.accounts.api.APICreateSessionForZCenterAccountEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "5.1.0"
		clz ErrorCode.class
	}
}
