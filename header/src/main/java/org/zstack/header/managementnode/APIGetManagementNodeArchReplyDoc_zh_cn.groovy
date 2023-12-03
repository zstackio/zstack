package org.zstack.header.managementnode

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取管理节点系统架构的返回"

	field {
		name "architecture"
		desc ""
		type "String"
		since "4.1.2"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.1.2"
	}
	ref {
		name "error"
		path "org.zstack.header.managementnode.APIGetManagementNodeArchReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.1.2"
		clz ErrorCode.class
	}
}
