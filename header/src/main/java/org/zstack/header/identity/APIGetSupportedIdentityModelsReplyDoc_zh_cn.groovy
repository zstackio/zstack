package org.zstack.header.identity

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取支持的账户类型的结果"

	field {
		name "configs"
		desc ""
		type "List"
		since "3.7.0"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "3.7.0"
	}
	ref {
		name "error"
		path "org.zstack.header.identity.APIGetSupportedIdentityModelsReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.7.0"
		clz ErrorCode.class
	}
}
