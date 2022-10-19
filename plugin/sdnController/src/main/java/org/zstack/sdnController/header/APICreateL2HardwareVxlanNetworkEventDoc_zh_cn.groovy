package org.zstack.sdnController.header

import org.zstack.header.errorcode.ErrorCode
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory

doc {

	title "VXLAN网路清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.sdnController.header.APICreateL2HardwareVxlanNetworkEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.7"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.sdnController.header.APICreateL2HardwareVxlanNetworkEvent.inventory"
		desc "null"
		type "L2VxlanNetworkInventory"
		since "3.7"
		clz L2VxlanNetworkInventory.class
	}
}
