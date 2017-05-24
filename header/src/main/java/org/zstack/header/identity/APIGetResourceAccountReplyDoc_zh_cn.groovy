package org.zstack.header.identity

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.identity.AccountInventory

doc {

	title "账户清单"

	ref {
		name "error"
		path "org.zstack.header.identity.APIGetResourceAccountReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.identity.APIGetResourceAccountReply.inventories"
		desc "null"
		type "Map"
		since "0.6"
		clz AccountInventory.class
	}
}
