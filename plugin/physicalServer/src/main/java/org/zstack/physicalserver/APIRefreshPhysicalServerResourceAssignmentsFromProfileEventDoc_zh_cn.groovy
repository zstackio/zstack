package org.zstack.physicalserver

import org.zstack.physicalserver.RefreshPhysicalServerResourceAssignmentResult
import org.zstack.header.errorcode.ErrorCode

doc {

	title "刷新物理服务器资源分配的返回"

	ref {
		name "results"
		path "org.zstack.physicalserver.APIRefreshPhysicalServerResourceAssignmentsFromProfileEvent.results"
		desc "各物理服务器的刷新执行结果，单台失败不影响其他服务器"
		type "List"
		since "5.5.38"
		clz RefreshPhysicalServerResourceAssignmentResult.class
	}
	field {
		name "success"
		desc "请求是否处理成功，各服务器的执行结果请查看results"
		type "boolean"
		since "5.5.38"
	}
	ref {
		name "error"
		path "org.zstack.physicalserver.APIRefreshPhysicalServerResourceAssignmentsFromProfileEvent.error"
		desc "请求级错误，服务器执行失败在results中返回",false
		type "ErrorCode"
		since "5.5.38"
		clz ErrorCode.class
	}
}
