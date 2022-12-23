package org.zstack.header.vm

import java.lang.Integer
import org.zstack.header.network.l3.UsedIpInventory
import java.lang.Integer
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "云主机网卡清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "0.6"
	}
	field {
		name "vmInstanceUuid"
		desc "云主机UUID"
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
		name "ip"
		desc "IP地址"
		type "String"
		since "0.6"
	}
	field {
		name "mac"
		desc "MAC地址"
		type "String"
		since "0.6"
	}
	field {
		name "hypervisorType"
		desc "虚拟化类型"
		type "String"
		since "0.6"
	}
	field {
		name "netmask"
		desc "子网掩码"
		type "String"
		since "0.6"
	}
	field {
		name "gateway"
		desc "网关"
		type "String"
		since "0.6"
	}
	field {
		name "metaData"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "ipVersion"
		desc "IP地址版本"
		type "Integer"
		since "0.6"
	}
	ref {
		name "usedIps"
		path "org.zstack.header.vm.VmNicInventory.usedIps"
		desc "null"
		type "List"
		since "0.6"
		clz UsedIpInventory.class
	}
	field {
		name "deviceId"
		desc "设备ID"
		type "Integer"
		since "0.6"
	}
	field {
		name "type"
		desc "网卡类型"
		type "String"
		since "0.6"
	}
	field {
		name "state"
		desc "网卡状态"
		type "String"
		since "4.5"
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
