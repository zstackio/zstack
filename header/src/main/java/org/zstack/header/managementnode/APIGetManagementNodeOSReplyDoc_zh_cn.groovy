package org.zstack.header.managementnode

import org.zstack.header.errorcode.ErrorCode

doc {

	title "在这里输入结构的名称"

	field {
		name "name"
		desc "资源名称"
		type "String"
		since "0.6"
	}
	field {
		name "version"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.managementnode.APIGetManagementNodeOSReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
}
