package org.zstack.header.identity.role

import java.sql.Timestamp
import java.sql.Timestamp
import org.zstack.header.identity.PolicyStatement

doc {

	title "在这里输入结构的名称"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "0.6"
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
		name "statement"
		path "org.zstack.header.identity.role.RolePolicyStatementInventory.statement"
		desc "null"
		type "PolicyStatement"
		since "0.6"
		clz PolicyStatement.class
	}
}
