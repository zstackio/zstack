package org.zstack.header.identity.role

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

	field {
		name "roleUuid"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "policyUuid"
		desc "权限策略UUID"
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
}
