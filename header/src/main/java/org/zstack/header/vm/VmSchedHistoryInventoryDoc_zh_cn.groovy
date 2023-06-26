package org.zstack.header.vm

import java.lang.Boolean
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

	field {
		name "vmInstanceUuid"
		desc "云主机UUID"
		type "String"
		since "0.6"
	}
	field {
		name "accountUuid"
		desc "账户UUID"
		type "String"
		since "0.6"
	}
	field {
		name "schedType"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "success"
		desc ""
		type "Boolean"
		since "0.6"
	}
	field {
		name "lastHostUuid"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "destHostUuid"
		desc ""
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
	field {
		name "zoneUuid"
		desc "区域UUID"
		type "String"
		since "0.6"
	}
}
