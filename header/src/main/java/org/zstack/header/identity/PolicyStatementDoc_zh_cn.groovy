package org.zstack.header.identity

doc {

	title "在这里输入结构的名称"

	field {
		name "name"
		desc "资源名称"
		type "String"
		since "0.6"
	}
	ref {
		name "effect"
		path "org.zstack.header.identity.PolicyStatement.effect"
		desc "null"
		type "PolicyStatementEffect"
		since "0.6"
		clz PolicyStatementEffect.class
	}
	field {
		name "principals"
		desc ""
		type "List"
		since "0.6"
	}
	field {
		name "requiredActions"
		desc "条件 Allow 同时要求具备的完整 API 权限列表；其全部 actions 必须带 @required 后缀且本列表非空，否则拒绝该条件授权。普通 Allow 不带后缀且列表为空时保持无条件授权。"
		type "List"
		since "5.5.38"
	}
	field {
		name "actions"
		desc ""
		type "List"
		since "0.6"
	}
	field {
		name "resources"
		desc ""
		type "List"
		since "0.6"
	}
}
