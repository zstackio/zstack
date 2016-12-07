package org.zstack.header.identity

import java.lang.Long
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

	field {
		name "name"
		desc "资源名称"
		type "String"
		since "0.6"
	}
	field {
		name "identityUuid"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "identityType"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "value"
		desc ""
		type "Long"
		since "0.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "0.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "0.6"
	}
}
