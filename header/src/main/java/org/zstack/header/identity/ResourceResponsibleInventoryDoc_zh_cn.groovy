package org.zstack.header.identity

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "资源责任关系实体类"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "4.4.52"
	}
	field {
		name "resourceUuid"
		desc "资源UUID"
		type "String"
		since "4.4.52"
	}
	field {
		name "responsibleType"
		desc ""
		type "String"
		since "4.4.52"
	}
	field {
		name "responsibleUuid"
		desc ""
		type "String"
		since "4.4.52"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "4.4.52"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "4.4.52"
	}
}
