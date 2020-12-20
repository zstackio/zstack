package org.zstack.header.network.l2

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.cluster.ClusterInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取二层网络允许加载的集群返回值"

	ref {
		name "error"
		path "org.zstack.header.network.l2.APIGetCandidateClusterForAttachingL2Reply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.0.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.network.l2.APIGetCandidateClusterForAttachingL2Reply.inventories"
		desc "null"
		type "List"
		since "4.0.0"
		clz ClusterInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.0.0"
	}
	ref {
		name "error"
		path "org.zstack.header.network.l2.APIGetCandidateClusterForAttachingL2Reply.error"
		desc "null"
		type "ErrorCode"
		since "4.0.0"
		clz ErrorCode.class
	}
}
