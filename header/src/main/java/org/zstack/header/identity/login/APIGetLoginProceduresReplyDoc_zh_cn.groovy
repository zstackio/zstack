package org.zstack.header.identity.login

import org.zstack.header.identity.login.LoginAuthenticationProcedureDesc
import org.zstack.header.errorcode.ErrorCode

doc {

	title "登录的认证步骤的返回"

	ref {
		name "procedures"
		path "org.zstack.header.identity.login.APIGetLoginProceduresReply.procedures"
		desc "null"
		type "List"
		since "4.6.0"
		clz LoginAuthenticationProcedureDesc.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.6.0"
	}
	ref {
		name "error"
		path "org.zstack.header.identity.login.APIGetLoginProceduresReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.6.0"
		clz ErrorCode.class
	}
}
