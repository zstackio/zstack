package org.zstack.sdnController.header

import org.zstack.header.network.sdncontroller.SdnControllerInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "重连SDN控制器清单"

	ref {
		name "inventory"
		path "org.zstack.sdnController.header.APIReconnectSdnControllerEvent.inventory"
		desc "null"
		type "SdnControllerInventory"
		since "5.3.0"
		clz SdnControllerInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.3.0"
	}
	ref {
		name "error"
		path "org.zstack.sdnController.header.APIReconnectSdnControllerEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.3.0"
		clz ErrorCode.class
	}
}
