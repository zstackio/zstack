package org.zstack.header.securitymachine.api

import org.zstack.header.errorcode.ErrorCode

doc {

	title "登录前获取相关配置信息结果"

	field {
		name "configs"
		desc "配置信息"
		type "Map"
		since "4.4.52"
	}
	field {
		name "success"
		desc "是否成功"
		type "boolean"
		since "4.4.52"
	}
	ref {
		name "error"
		path "org.zstack.header.securitymachine.api.APIGetPreAuthenticationConfigReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
}
