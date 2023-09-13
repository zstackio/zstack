package org.zstack.header.tag

import org.zstack.header.tag.SystemTagInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "标签清单"

	ref {
		name "inventories"
		path "org.zstack.header.tag.APICreateSystemTagsEvent.inventories"
		desc "null"
		type "List"
		since "4.7.0"
		clz SystemTagInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.7.0"
	}
	ref {
		name "error"
		path "org.zstack.header.tag.APICreateSystemTagsEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.7.0"
		clz ErrorCode.class
	}
}
