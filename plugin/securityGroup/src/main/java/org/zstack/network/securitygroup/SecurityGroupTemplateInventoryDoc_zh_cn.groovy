package org.zstack.network.securitygroup

import org.zstack.network.securitygroup.SecurityGroupTemplateRule

doc {

	title "内置安全组模板"

	field {
		name "id"
		desc "模板标识"
		type "String"
		since "5.7.0"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "5.7.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "5.7.0"
	}
	field {
		name "defaultSelected"
		desc "是否默认选中，仅自定义模板为 true"
		type "boolean"
		since "5.7.0"
	}
	ref {
		name "rules"
		path "org.zstack.network.securitygroup.SecurityGroupTemplateInventory.rules"
		desc "模板预填的用户规则，不包含系统规则"
		type "List"
		since "5.7.0"
		clz SecurityGroupTemplateRule.class
	}
}
