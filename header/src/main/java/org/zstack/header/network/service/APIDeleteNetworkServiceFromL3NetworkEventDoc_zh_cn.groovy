package org.zstack.header.network.service

import org.zstack.header.network.l3.L3NetworkInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "在这里输入结构的名称"

	ref {
		name "inventory"
		path "org.zstack.header.network.service.APIDeleteNetworkServiceFromL3NetworkEvent.inventory"
		desc "null"
		type "L3NetworkInventory"
		since "5.1.0"
		clz L3NetworkInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.1.0"
	}
	ref {
		name "error"
		path "org.zstack.header.network.service.APIDeleteNetworkServiceFromL3NetworkEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.1.0"
		clz ErrorCode.class
	}
}
