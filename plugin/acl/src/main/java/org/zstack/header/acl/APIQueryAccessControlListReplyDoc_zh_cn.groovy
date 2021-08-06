package org.zstack.header.acl

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.acl.AccessControlListInventory

doc {

	title "访问控制策略组清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.acl.APIQueryAccessControlListReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.9"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.acl.APIQueryAccessControlListReply.inventories"
		desc "null"
		type "List"
		since "3.9"
		clz AccessControlListInventory.class
	}
}
