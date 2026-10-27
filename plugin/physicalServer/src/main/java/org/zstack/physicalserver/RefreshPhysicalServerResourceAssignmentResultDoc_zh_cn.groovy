package org.zstack.physicalserver

import org.zstack.header.errorcode.ErrorCode

doc {

	title "单台物理服务器的资源分配刷新结果"

	field {
		name "serverUuid"
		desc "物理服务器UUID"
		type "String"
		since "5.5.38"
	}
	field {
		name "success"
		desc "该服务器刷新执行是否成功；等待服务重启仍视为成功，是否生效由分配状态和restartRequired表示"
		type "boolean"
		since "5.5.38"
	}
	ref {
		name "error"
		path "org.zstack.physicalserver.RefreshPhysicalServerResourceAssignmentResult.error"
		desc "该服务器刷新失败的首个错误，成功时为null；超时不表示远端操作已取消",false
		type "ErrorCode"
		since "5.5.38"
		clz ErrorCode.class
	}
}
