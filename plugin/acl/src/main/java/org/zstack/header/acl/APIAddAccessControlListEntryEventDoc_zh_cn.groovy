package org.zstack.header.acl

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.acl.AccessControlListEntryInventory

doc {

	title "访问控制策略组的IP清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.acl.APIAddAccessControlListEntryEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.9"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.acl.APIAddAccessControlListEntryEvent.inventory"
		desc "null"
		type "AccessControlListEntryInventory"
		since "3.9"
		clz AccessControlListEntryInventory.class
	}
}
