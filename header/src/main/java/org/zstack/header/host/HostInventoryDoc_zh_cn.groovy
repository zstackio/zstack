package org.zstack.header.host

import java.lang.Long
import java.lang.Long
import java.lang.Long
import java.lang.Long
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

	field {
		name "zoneUuid"
		desc "区域UUID"
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
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "0.6"
	}
	field {
		name "clusterUuid"
		desc "集群UUID"
		type "String"
		since "0.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "0.6"
	}
	field {
		name "managementIp"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "hypervisorType"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "state"
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
		name "totalCpuCapacity"
		desc ""
		type "Long"
		since "0.6"
	}
	field {
		name "availableCpuCapacity"
		desc ""
		type "Long"
		since "0.6"
	}
	field {
		name "totalMemoryCapacity"
		desc ""
		type "Long"
		since "0.6"
	}
	field {
		name "availableMemoryCapacity"
		desc ""
		type "Long"
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
