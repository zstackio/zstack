package org.zstack.header.vm.cdrom

import java.lang.Integer
import java.sql.Timestamp

doc {

	title "云主机CDROM"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "4.7.13"
	}
	field {
		name "vmInstanceUuid"
		desc "云主机UUID"
		type "String"
		since "4.7.13"
	}
	field {
		name "deviceId"
		desc ""
		type "Integer"
		since "4.7.13"
	}
	field {
		name "isoUuid"
		desc ""
		type "String"
		since "4.7.13"
	}
	field {
		name "isoInstallPath"
		desc ""
		type "String"
		since "4.7.13"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "4.7.13"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "4.7.13"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "4.7.13"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "4.7.13"
	}
}
