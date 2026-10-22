package org.zstack.network.securitygroup

import org.zstack.network.securitygroup.SecurityGroupTemplateInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "内置安全组模板查询结果"

	ref {
		name "templates"
		path "org.zstack.network.securitygroup.APIGetSecurityGroupTemplatesReply.templates"
		desc "内置安全组模板列表"
		type "List"
		since "5.7.0"
		clz SecurityGroupTemplateInventory.class
	}
	field {
		name "success"
		desc "操作是否成功"
		type "boolean"
		since "5.7.0"
	}
	ref {
		name "error"
		path "org.zstack.network.securitygroup.APIGetSecurityGroupTemplatesReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.7.0"
		clz ErrorCode.class
	}
}
