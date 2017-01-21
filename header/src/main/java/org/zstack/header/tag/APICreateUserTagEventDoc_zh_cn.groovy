package org.zstack.header.tag

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.tag.UserTagInventory

doc {

	title "标签清单"

	ref {
		name "error"
		path "org.zstack.header.tag.APICreateUserTagEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.tag.APICreateUserTagEvent.inventory"
		desc "null"
		type "UserTagInventory"
		since "0.6"
		clz UserTagInventory.class
	}
}
