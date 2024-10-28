package org.zstack.header.host

import java.lang.Boolean
import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "5.2.1"
	}
	field {
		name "serviceType"
		desc ""
		type "String"
		since "5.2.1"
	}
	field {
		name "system"
		desc ""
		type "Boolean"
		since "5.2.1"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "5.2.1"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "5.2.1"
	}
}
