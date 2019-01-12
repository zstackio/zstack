package org.zstack.header.vm.cdrom

import java.lang.Integer
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "云主机CDROM"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.3"
	}
	field {
		name "vmInstanceUuid"
		desc "云主机UUID"
		type "String"
		since "3.3"
	}
	field {
		name "deviceId"
		desc "光驱顺序号"
		type "Integer"
		since "3.3"
	}
	field {
		name "isoUuid"
		desc "ISO镜像UUID"
		type "String"
		since "3.3"
	}
	field {
		name "isoInstallPath"
		desc "ISO镜像挂载路径"
		type "String"
		since "3.3"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.3"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "3.3"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.3"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.3"
	}
}
