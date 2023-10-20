package org.zstack.network.hostNetworkInterface.lldp.api

import org.zstack.network.hostNetworkInterface.lldp.entity.HostNetworkInterfaceLldpInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询物理网口lldp配置"

	ref {
		name "inventories"
		path "org.zstack.network.hostNetworkInterface.lldp.api.APIQueryHostNetworkInterfaceLldpReply.inventories"
		desc "物理网卡lldp配置清单"
		type "List"
		since "5.0.0"
		clz HostNetworkInterfaceLldpInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.0.0"
	}
	ref {
		name "error"
		path "org.zstack.network.hostNetworkInterface.lldp.api.APIQueryHostNetworkInterfaceLldpReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.0.0"
		clz ErrorCode.class
	}
}
