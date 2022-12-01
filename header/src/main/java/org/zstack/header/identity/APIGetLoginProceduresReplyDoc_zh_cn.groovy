package org.zstack.header.identity

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取指定用户的附加登录方式的结果"

	field {
		name "additions"
		desc "用户的附加登录方式，表示在普通的用户名口令以外，还需要哪些认证需要"
		type "List"
		since "4.5.1"
	}
	field {
		name "success"
		desc "结果请求是否成功"
		type "boolean"
		since "4.5.1"
	}
	ref {
		name "error"
		path "org.zstack.header.identity.APIGetLoginProceduresReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.5.1"
		clz ErrorCode.class
	}
}
