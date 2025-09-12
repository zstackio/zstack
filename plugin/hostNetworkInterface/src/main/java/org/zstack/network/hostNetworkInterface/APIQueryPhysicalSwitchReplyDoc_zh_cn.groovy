package org.zstack.network.hostNetworkInterface

import org.zstack.network.hostNetworkInterface.PhysicalSwitchInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "query physical switch"

	ref {
		name "inventories"
		path "org.zstack.network.hostNetworkInterface.APIQueryPhysicalSwitchReply.inventories"
		desc "List of physical switch inventories returned by the query"
		type "List"
		since "5.3.28"
		clz PhysicalSwitchInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.3.28"
	}
	ref {
		name "error"
		path "org.zstack.network.hostNetworkInterface.APIQueryPhysicalSwitchReply.error"
		desc "Error code, null indicates success, non-null indicates operation failure"
		type "ErrorCode"
		since "5.3.28"
		clz ErrorCode.class
	}
}
