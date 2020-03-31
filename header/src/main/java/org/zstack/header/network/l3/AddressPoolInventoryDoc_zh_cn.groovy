package org.zstack.header.network.l3

import java.lang.Integer
import java.lang.Integer
import org.zstack.header.network.l3.IpRangeType
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "地址池结构清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.9"
	}
	field {
		name "l3NetworkUuid"
		desc "三层网络UUID"
		type "String"
		since "3.9"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "0.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "3.9"
	}
	field {
		name "startIp"
		desc "起始IP"
		type "String"
		since "3.9"
	}
	field {
		name "endIp"
		desc "结束IP"
		type "String"
		since "3.9"
	}
	field {
		name "netmask"
		desc "掩码"
		type "String"
		since "3.9"
	}
	field {
		name "gateway"
		desc ""
		type "String"
		since "3.9"
	}
	field {
		name "networkCidr"
		desc ""
		type "String"
		since "3.9"
	}
	field {
		name "ipVersion"
		desc ""
		type "Integer"
		since "3.9"
	}
	field {
		name "addressMode"
		desc ""
		type "String"
		since "3.9"
	}
	field {
		name "prefixLen"
		desc ""
		type "Integer"
		since "3.9"
	}
	ref {
		name "ipRangeType"
		path "org.zstack.header.network.l3.AddressPoolInventory.ipRangeType"
		desc "null"
		type "IpRangeType"
		since "3.9"
		clz IpRangeType.class
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.9"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.9"
	}
}
