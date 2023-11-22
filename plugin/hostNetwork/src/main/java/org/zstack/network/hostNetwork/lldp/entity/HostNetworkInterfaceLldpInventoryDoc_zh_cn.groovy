package org.zstack.network.hostNetwork.lldp.entity

import java.sql.Timestamp
import org.zstack.network.hostNetwork.lldp.entity.HostNetworkInterfaceLldpRefInventory

doc {

	title "物理网卡lldp配置清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "5.0.0"
	}
	field {
		name "interfaceUuid"
		desc "物理网口Uuid"
		type "String"
		since "5.0.0"
	}
	field {
		name "mode"
		desc "lldp工作模式"
		type "String"
		since "5.0.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "5.0.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "5.0.0"
	}
	ref {
		name "lldp"
		path "org.zstack.network.hostNetwork.lldp.entity.HostNetworkInterfaceLldpInventory.lldp"
		desc "物理网口lldp配置清单"
		type "HostNetworkInterfaceLldpRefInventory"
		since "5.0.0"
		clz HostNetworkInterfaceLldpRefInventory.class
	}
}
