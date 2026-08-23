package org.zstack.physicalserver

import org.zstack.physicalserver.PhysicalServerResourceAssignmentInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询物理服务器资源分配的返回"

	ref {
		name "inventories"
		path "org.zstack.physicalserver.APIQueryPhysicalServerResourceAssignmentReply.inventories"
		desc "物理服务器资源分配清单"
		type "List"
		since "5.5.38"
		clz PhysicalServerResourceAssignmentInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.5.38"
	}
	ref {
		name "error"
		path "org.zstack.physicalserver.APIQueryPhysicalServerResourceAssignmentReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.5.38"
		clz ErrorCode.class
	}
}
