package org.zstack.header.network.l3

import java.lang.Integer
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "0.6"
	}
	field {
		name "ipRangeUuid"
		desc "IP段UUID"
		type "String"
		since "0.6"
	}
	field {
		name "l3NetworkUuid"
		desc "三层网络UUID"
		type "String"
		since "0.6"
	}
	field {
		name "ipVersion"
		desc ""
		type "Integer"
		since "0.6"
	}
	field {
		name "ip"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "netmask"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "gateway"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "usedFor"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "ipInLong"
		desc ""
		type "long"
		since "0.6"
	}
	field {
		name "vmNicUuid"
		desc "云主机网卡UUID"
		type "String"
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
