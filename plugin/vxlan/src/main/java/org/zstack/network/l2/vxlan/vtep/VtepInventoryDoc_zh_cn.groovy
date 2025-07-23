package org.zstack.network.l2.vxlan.vtep

import java.lang.Integer
import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "5.3.28"
	}
	field {
		name "hostUuid"
		desc "物理机UUID"
		type "String"
		since "5.3.28"
	}
	field {
		name "vtepIp"
		desc ""
		type "String"
		since "5.3.28"
	}
	field {
		name "port"
		desc ""
		type "Integer"
		since "5.3.28"
	}
	field {
		name "type"
		desc ""
		type "String"
		since "5.3.28"
	}
	field {
		name "physicalInterface"
		desc ""
		type "String"
		since "5.3.28"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "5.3.28"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "5.3.28"
	}
	field {
		name "poolUuid"
		desc ""
		type "String"
		since "5.3.28"
	}
}
