package org.zstack.directory

import org.zstack.directory.DirectoryInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "更新目录名称返回"

	ref {
		name "inventory"
		path "org.zstack.directory.APIUpdateDirectoryEvent.inventory"
		desc "null"
		type "DirectoryInventory"
		since "4.6.0"
		clz DirectoryInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.6.0"
	}
	ref {
		name "error"
		path "org.zstack.directory.APIUpdateDirectoryEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.6.0"
		clz ErrorCode.class
	}
}
