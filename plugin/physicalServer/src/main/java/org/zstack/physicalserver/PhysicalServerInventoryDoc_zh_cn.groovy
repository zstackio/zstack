package org.zstack.physicalserver

import java.sql.Timestamp

doc {

	title "物理服务器清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "5.5.38"
	}
	field {
		name "zoneUuid"
		desc "区域UUID"
		type "String"
		since "5.5.38"
	}
	field {
		name "serialNumber"
		desc "经标准化的服务器序列号，在云平台范围内唯一"
		type "String"
		since "5.5.38"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "5.5.38"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "5.5.38"
	}
}
