package org.zstack.header.network.l2

import org.zstack.header.network.l2.L2NetworkInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "在这里输入结构的名称"

	ref {
		name "inventory"
		path "org.zstack.header.network.l2.APIChangeL2NetworkVlanIdEvent.inventory"
		desc "null"
		type "L2NetworkInventory"
		since "5.1.0"
		clz L2NetworkInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.1.0"
	}
	ref {
		name "error"
		path "org.zstack.header.network.l2.APIChangeL2NetworkVlanIdEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.1.0"
		clz ErrorCode.class
	}
}
