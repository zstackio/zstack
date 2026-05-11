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
		name "zoneUuid"
		desc "区域UUID"
		type "String"
		since "5.5.16"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "5.5.16"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "5.5.16"
	}
	field {
		name "type"
		desc ""
		type "String"
		since "5.5.16"
	}
	field {
		name "dhcpInterface"
		desc ""
		type "String"
		since "5.5.16"
	}
	field {
		name "dhcpRangeStartIp"
		desc ""
		type "String"
		since "5.5.16"
	}
	field {
		name "dhcpRangeEndIp"
		desc ""
		type "String"
		since "5.5.16"
	}
	field {
		name "dhcpRangeNetmask"
		desc ""
		type "String"
		since "5.5.16"
	}
	field {
		name "dhcpRangeGateway"
		desc ""
		type "String"
		since "5.5.16"
	}
	field {
		name "state"
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
	field {
		name "attachedClusterUuids"
		desc ""
		type "List"
		since "5.5.16"
	}
	field {
		name "attachedPoolUuids"
		desc ""
		type "List"
		since "5.5.16"
	}
}
