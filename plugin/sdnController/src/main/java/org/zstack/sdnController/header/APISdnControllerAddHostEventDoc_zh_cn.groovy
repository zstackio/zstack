package org.zstack.sdnController.header

import org.zstack.header.network.sdncontroller.SdnControllerInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "SDN控制器添加物理机清单"

	ref {
		name "inventory"
		path "org.zstack.sdnController.header.APISdnControllerAddHostEvent.inventory"
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
		path "org.zstack.sdnController.header.APISdnControllerAddHostEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.3.0"
		clz ErrorCode.class
	}
}
