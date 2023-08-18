package org.zstack.sdnController.header

import org.zstack.header.errorcode.ErrorCode

doc {

	title "硬件VXLAN资源池清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.sdnController.header.APICreateL2HardwareVxlanNetworkPoolEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.7"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.sdnController.header.APICreateL2HardwareVxlanNetworkPoolEvent.inventory"
		desc "null"
		type "HardwareL2VxlanNetworkPoolInventory"
		since "3.7"
		clz HardwareL2VxlanNetworkPoolInventory.class
	}
}
