package org.zstack.core.notification

import java.lang.Long
import java.lang.Object
import java.sql.Timestamp
import java.sql.Timestamp

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
		name "content"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "arguments"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "sender"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "status"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "resourceUuid"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "resourceType"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "type"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "time"
		desc ""
		type "Long"
		since "0.6"
	}
	field {
		name "opaque"
		desc ""
		type "Object"
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
