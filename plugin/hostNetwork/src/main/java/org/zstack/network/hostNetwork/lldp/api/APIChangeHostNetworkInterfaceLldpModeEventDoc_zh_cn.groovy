package org.zstack.network.hostNetwork.lldp.api

import org.zstack.network.hostNetwork.lldp.entity.HostNetworkInterfaceLldpInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "修改lldp的工作模式"

	ref {
		name "inventories"
		path "org.zstack.network.hostNetwork.lldp.api.APIChangeHostNetworkInterfaceLldpModeEvent.inventories"
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
		path "org.zstack.network.hostNetwork.lldp.api.APIChangeHostNetworkInterfaceLldpModeEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.0.0"
		clz ErrorCode.class
	}
}
