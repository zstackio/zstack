package org.zstack.physicalserver

import org.zstack.physicalserver.PhysicalServerManagedServiceInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查看物理服务器管控服务资源使用结果"

	ref {
		name "services"
		path "org.zstack.physicalserver.APIGetPhysicalServerManagedServicesReply.services"
		desc "物理服务器上由各Role观测到的管控服务资源使用清单"
		type "List"
		since "5.5.38"
		clz PhysicalServerManagedServiceInventory.class
	}
	field {
		name "success"
		desc "API调用是否成功"
		type "boolean"
		since "5.5.38"
	}
	ref {
		name "error"
		path "org.zstack.physicalserver.APIGetPhysicalServerManagedServicesReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.5.38"
		clz ErrorCode.class
	}
}
