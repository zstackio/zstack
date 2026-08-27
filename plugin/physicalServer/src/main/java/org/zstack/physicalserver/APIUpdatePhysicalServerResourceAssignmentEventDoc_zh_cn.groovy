package org.zstack.physicalserver

import org.zstack.physicalserver.PhysicalServerResourceAssignmentInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "更新物理服务器资源分配的返回"

	ref {
		name "inventory"
		path "org.zstack.physicalserver.APIUpdatePhysicalServerResourceAssignmentEvent.inventory"
		desc "已持久化的资源分配清单；实际状态可能仍在异步收敛"
		type "PhysicalServerResourceAssignmentInventory"
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
		path "org.zstack.physicalserver.APIUpdatePhysicalServerResourceAssignmentEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.5.38"
		clz ErrorCode.class
	}
}
