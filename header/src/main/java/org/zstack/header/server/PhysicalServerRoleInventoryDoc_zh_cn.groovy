package org.zstack.header.server

import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "5.5.16"
	}
	field {
		name "serverUuid"
		desc ""
		type "String"
		since "5.5.16"
	}
	field {
		name "roleType"
		desc ""
		type "String"
		since "5.5.16"
	}
	field {
		name "roleUuid"
		desc ""
		type "String"
		since "5.5.16"
	}
	field {
		name "schedulingMode"
		desc ""
		type "String"
		since "5.5.16"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "5.5.16"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "5.5.16"
	}
}
