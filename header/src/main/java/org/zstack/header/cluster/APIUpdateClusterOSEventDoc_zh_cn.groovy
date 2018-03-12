package org.zstack.header.cluster

import org.zstack.header.errorcode.ErrorCode

doc {

	title "集群内物理机操作系统升级结果"

	ref {
		name "error"
		path "org.zstack.header.cluster.APIUpdateClusterOSEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.3"
		clz ErrorCode.class
	}
	field {
		name "results"
		desc "集群内物理机操作系统升级结果"
		type "Map"
		since "2.3"
	}
}
