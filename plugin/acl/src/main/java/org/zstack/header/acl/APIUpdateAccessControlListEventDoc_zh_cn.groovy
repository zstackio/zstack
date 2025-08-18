package org.zstack.header.acl

import org.zstack.header.acl.AccessControlListInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "访问控制策略组清单"

	ref {
		name "inventory"
		path "org.zstack.header.acl.APIUpdateAccessControlListEvent.inventory"
		desc "更新后的访问控制策略组详细信息"
		type "AccessControlListInventory"
		since "5.3.28"
		clz AccessControlListInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.3.28"
	}
	ref {
		name "error"
		path "org.zstack.header.acl.APIUpdateAccessControlListEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.3.28"
		clz ErrorCode.class
	}
}
