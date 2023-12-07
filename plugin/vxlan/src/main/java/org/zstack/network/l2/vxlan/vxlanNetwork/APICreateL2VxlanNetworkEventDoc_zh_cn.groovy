package org.zstack.network.l2.vxlan.vxlanNetwork

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
		path "org.zstack.network.l2.vxlan.vxlanNetwork.APICreateL2VxlanNetworkEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.network.l2.vxlan.vxlanNetwork.APICreateL2VxlanNetworkEvent.inventory"
		desc "null"
		type "L2VxlanNetworkInventory"
		since "0.6"
		clz L2VxlanNetworkInventory.class
	}
}
