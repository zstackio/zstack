package org.zstack.header.network.l2

import org.zstack.header.network.l2.L2NetworkInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "二层网络清单"

	ref {
		name "inventory"
		path "org.zstack.header.network.l2.APIAttachL2NetworkToHostEvent.inventory"
		desc "null"
		type "L2NetworkInventory"
		since "4.0.1"
		clz L2NetworkInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.0.1"
	}
	ref {
		name "error"
		path "org.zstack.header.network.l2.APIAttachL2NetworkToHostEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.0.1"
		clz ErrorCode.class
	}
}
