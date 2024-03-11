package org.zstack.header.volume

import java.sql.Timestamp

doc {

	title "硬盘模板"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "zsv 4.2.0"
	}
	field {
		name "volumeUuid"
		desc "云盘UUID"
		type "String"
		since "zsv 4.2.0"
	}
	field {
		name "originalType"
		desc "原来的硬盘类型"
		type "String"
		since "zsv 4.2.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 4.2.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv 4.2.0"
	}
}
