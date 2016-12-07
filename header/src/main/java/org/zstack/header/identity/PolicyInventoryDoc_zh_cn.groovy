package org.zstack.header.identity

import org.zstack.header.identity.PolicyInventory.Statement

doc {

	title "在这里输入结构的名称"

	ref {
		name "statements"
		path "org.zstack.header.identity.PolicyInventory.statements"
		desc "null"
		type "List"
		since "0.6"
		clz Statement.class
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "0.6"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "0.6"
	}
	field {
		name "accountUuid"
		desc "账户UUID"
		type "String"
		since "0.6"
	}
}
