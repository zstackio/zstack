package org.zstack.header.tag

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.tag.UserTagInventory

doc {

	title "标签清单"

	ref {
		name "error"
		path "org.zstack.header.tag.APIQueryUserTagReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.tag.APIQueryUserTagReply.inventories"
		desc "null"
		type "List"
		since "0.6"
		clz UserTagInventory.class
	}
}
