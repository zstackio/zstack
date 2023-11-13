package org.zstack.network.hostNetwork.lldp.api

import org.zstack.network.hostNetwork.lldp.entity.HostNetworkInterfaceLldpRefInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取物理网口lldp信息"

	ref {
		name "lldp"
		path "org.zstack.network.hostNetwork.lldp.api.APIGetHostNetworkInterfaceLldpReply.lldp"
		desc "物理网卡lldp信息清单"
		type "HostNetworkInterfaceLldpRefInventory"
		since "5.0.0"
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
		path "org.zstack.network.hostNetwork.lldp.api.APIGetHostNetworkInterfaceLldpReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.0.0"
		clz ErrorCode.class
	}
}
