package org.zstack.header.managementnode

import org.zstack.header.errorcode.ErrorCode

doc {

	title "支持的API列表"

	field {
		name "supportApis"
		desc ""
		type "List"
		since "4.5.0"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.managementnode.APIGetSupportAPIsReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
}
