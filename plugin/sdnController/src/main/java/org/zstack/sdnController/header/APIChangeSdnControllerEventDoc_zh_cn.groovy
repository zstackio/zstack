package org.zstack.sdnController.header

import org.zstack.header.network.sdncontroller.SdnControllerInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "Upate SDN Controller"

	ref {
		name "inventory"
		path "org.zstack.sdnController.header.APIChangeSdnControllerEvent.inventory"
		desc "Updated SDN controller inventory after change operation"
		type "SdnControllerInventory"
		since "5.3.28"
		clz SdnControllerInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.3.28"
	}
	ref {
		name "error"
		path "org.zstack.sdnController.header.APIChangeSdnControllerEvent.error"
		desc "Error code, null if operation succeeds, non-null if operation fails",false
		type "ErrorCode"
		since "5.3.28"
		clz ErrorCode.class
	}
}
