package org.zstack.directory

import org.zstack.directory.DirectoryInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询目录分组的返回"

	ref {
		name "inventories"
		path "org.zstack.directory.APIQueryDirectoryReply.inventories"
		desc "null"
		type "List"
		since "4.7.0"
		clz DirectoryInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.7.0"
	}
	ref {
		name "error"
		path "org.zstack.directory.APIQueryDirectoryReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.7.0"
		clz ErrorCode.class
	}
}
