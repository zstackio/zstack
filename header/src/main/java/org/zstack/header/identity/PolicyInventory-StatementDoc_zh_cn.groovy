package org.zstack.header.identity

import org.zstack.header.identity.AccountConstant.StatementEffect

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
		path "org.zstack.header.identity.PolicyInventory.Statement.effect"
		desc "null"
		type "StatementEffect"
		since "0.6"
		clz StatementEffect.class
	}
	field {
		name "principals"
		desc ""
		type "List"
		since "0.6"
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
