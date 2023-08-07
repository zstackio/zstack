package org.zstack.network.l2.vxlan.vtep

import java.lang.Integer
import java.sql.Timestamp

doc {

	title "vxlan网络外部vtep地址"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "4.7.11"
	}
	field {
		name "vtepIp"
		desc ""
		type "String"
		since "4.7.11"
	}
	field {
		name "port"
		desc ""
		type "Integer"
		since "4.7.11"
	}
	field {
		name "type"
		desc ""
		type "String"
		since "4.7.11"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "4.7.11"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "4.7.11"
	}
	field {
		name "poolUuid"
		desc ""
		type "String"
		since "4.7.11"
	}
}
