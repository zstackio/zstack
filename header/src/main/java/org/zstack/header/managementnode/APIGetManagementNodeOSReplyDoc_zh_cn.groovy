package org.zstack.header.managementnode

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取管理节点系统的返回"

	field {
		name "name"
		desc "资源名称"
		type "String"
		since "4.1.2"
	}
	field {
		name "version"
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
		path "org.zstack.header.managementnode.APIGetManagementNodeOSReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.1.2"
		clz ErrorCode.class
	}
}
