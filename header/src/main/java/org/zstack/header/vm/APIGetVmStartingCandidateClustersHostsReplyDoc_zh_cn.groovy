package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.host.HostInventory
import org.zstack.header.cluster.ClusterInventory

doc {

	title "在这里输入结构的名称"

	ref {
		name "error"
		path "org.zstack.header.vm.APIGetVmStartingCandidateClustersHostsReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "hosts"
		path "org.zstack.header.vm.APIGetVmStartingCandidateClustersHostsReply.hostInventories"
		desc "null"
		type "List"
		since "0.6"
		clz HostInventory.class
	}
	ref {
		name "clusters"
		path "org.zstack.header.vm.APIGetVmStartingCandidateClustersHostsReply.clusterInventories"
		desc "null"
		type "List"
		since "0.6"
		clz ClusterInventory.class
	}
}
