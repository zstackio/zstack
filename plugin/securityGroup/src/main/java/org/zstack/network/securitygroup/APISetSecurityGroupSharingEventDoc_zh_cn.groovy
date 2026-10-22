package org.zstack.network.securitygroup

import org.zstack.header.errorcode.ErrorCode

doc {

	title "安全组共享设置结果"

	field {
		name "success"
		desc "操作是否成功"
		type "boolean"
		since "5.7.0"
	}
	ref {
		name "error"
		path "org.zstack.network.securitygroup.APISetSecurityGroupSharingEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.7.0"
		clz ErrorCode.class
	}
	field {
		name "securityGroupUuid"
		desc "安全组UUID"
		type "String"
		since "5.7.0"
	}
	field {
		name "mode"
		desc "共享模式：None 不共享，Specified 指定共享，Public 全局共享"
		type "String"
		since "5.7.0"
	}
	field {
		name "accountUuids"
		desc "指定共享的最终账户名单；项目使用 linkedAccountUuid，仅 Specified 模式可传"
		type "List"
		since "5.7.0"
	}
}
