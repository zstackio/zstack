package org.zstack.header.identity.role

import org.zstack.header.identity.role.RoleType
import org.zstack.header.identity.role.RoleState
import java.sql.Timestamp
import java.sql.Timestamp
import org.zstack.header.identity.role.RolePolicyStatementInventory
import org.zstack.header.identity.role.RolePolicyRefInventory

doc {

	title "在这里输入结构的名称"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "0.6"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "0.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "0.6"
	}
	field {
		name "identity"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "rootUuid"
		desc ""
		type "String"
		since "0.6"
	}
	ref {
		name "type"
		path "org.zstack.header.identity.role.RoleInventory.type"
		desc "null"
		type "RoleType"
		since "0.6"
		clz RoleType.class
	}
	ref {
		name "state"
		path "org.zstack.header.identity.role.RoleInventory.state"
		desc "null"
		type "RoleState"
		since "0.6"
		clz RoleState.class
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "0.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "0.6"
	}
	ref {
		name "statements"
		path "org.zstack.header.identity.role.RoleInventory.statements"
		desc "null"
		type "List"
		since "0.6"
		clz RolePolicyStatementInventory.class
	}
	ref {
		name "policies"
		path "org.zstack.header.identity.role.RoleInventory.policies"
		desc "null"
		type "List"
		since "0.6"
		clz RolePolicyRefInventory.class
	}
}
