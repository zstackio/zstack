package org.zstack.physicalserver

import org.zstack.physicalserver.PhysicalServerInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询物理服务器的返回"

	ref {
		name "inventories"
		path "org.zstack.physicalserver.APIQueryPhysicalServerReply.inventories"
		desc "物理服务器清单"
		type "List"
		since "5.5.38"
		clz PhysicalServerInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.5.38"
	}
	ref {
		name "error"
		path "org.zstack.physicalserver.APIQueryPhysicalServerReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.5.38"
		clz ErrorCode.class
	}
}
