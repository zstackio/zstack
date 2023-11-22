package org.zstack.network.hostNetwork.lldp.api

import org.zstack.network.hostNetwork.lldp.entity.HostNetworkInterfaceLldpRefInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询物理网口lldp信息"

	ref {
		name "inventory"
		path "org.zstack.network.hostNetwork.lldp.api.APIQueryHostNetworkInterfaceLldpRefReply.inventory"
		desc "null"
		type "HostNetworkInterfaceLldpRefInventory"
		since "4.8.0"
		clz HostNetworkInterfaceLldpRefInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.0.0"
	}
	ref {
		name "error"
		path "org.zstack.network.hostNetwork.lldp.api.APIQueryHostNetworkInterfaceLldpRefReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.0.0"
		clz ErrorCode.class
	}
}
