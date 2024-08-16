package org.zstack.header.storage.addon.backup

import java.lang.Long
import java.sql.Timestamp

doc {

	title "外部镜像存储"

	field {
		name "identity"
		desc ""
		type "String"
		since "5.0.0"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "5.0.0"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "5.0.0"
	}
	field {
		name "url"
		desc ""
		type "String"
		since "5.0.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "5.0.0"
	}
	field {
		name "totalCapacity"
		desc ""
		type "Long"
		since "5.0.0"
	}
	field {
		name "availableCapacity"
		desc ""
		type "Long"
		since "5.0.0"
	}
	field {
		name "type"
		desc ""
		type "String"
		since "5.0.0"
	}
	field {
		name "state"
		desc ""
		type "String"
		since "5.0.0"
	}
	field {
		name "status"
		desc ""
		type "String"
		since "5.0.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "5.0.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "5.0.0"
	}
	field {
		name "attachedZoneUuids"
		desc ""
		type "List"
		since "5.0.0"
	}
}
