package org.zstack.network.securitygroup

import org.zstack.header.errorcode.ErrorCode

doc {

	title "安全组规则可用性结果"

	field {
		name "available"
		desc "规则是否可用"
		type "boolean"
		since "4.7.11"
	}
	field {
		name "reason"
		desc "原因"
		type "String"
		since "4.7.21"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.7.21"
	}
	ref {
		name "error"
		path "org.zstack.network.securitygroup.APIValidateSecurityGroupRuleReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.7.21"
		clz ErrorCode.class
	}
}
