package org.zstack.header.network.l2


import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取集群允许加载的二层网络返回值"

	ref {
		name "error"
		path "org.zstack.header.network.l2.APIGetCandidateL2ForAttachingClusterReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.0.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.network.l2.APIGetCandidateL2ForAttachingClusterReply.inventories"
		desc "null"
		type "List"
		since "4.0.0"
		clz L2NetworkData.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.0.0"
	}
}
