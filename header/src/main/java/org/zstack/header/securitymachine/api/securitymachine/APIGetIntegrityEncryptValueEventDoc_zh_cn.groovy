package org.zstack.header.securitymachine.api.securitymachine

import org.zstack.header.errorcode.ErrorCode

doc {

	title "完整性加密结果"

	field {
		name "result"
		desc "加密结果"
		type "String"
		since "0.6"
	}
	field {
		name "success"
		desc "是否成功"
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.securitymachine.api.securitymachine.APIGetIntegrityEncryptValueEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
}
