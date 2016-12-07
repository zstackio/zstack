package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.zone.ZoneInventory
import org.zstack.header.cluster.ClusterInventory
import org.zstack.header.host.HostInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "在这里输入结构的名称"

	ref {
		name "error"
		path "org.zstack.header.vm.APIGetCandidateZonesClustersHostsForCreatingVmReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "zones"
		path "org.zstack.header.vm.APIGetCandidateZonesClustersHostsForCreatingVmReply.zones"
		desc "null"
		type "List"
		since "0.6"
		clz ZoneInventory.class
	}
	ref {
		name "clusters"
		path "org.zstack.header.vm.APIGetCandidateZonesClustersHostsForCreatingVmReply.clusters"
		desc "null"
		type "List"
		since "0.6"
		clz ClusterInventory.class
	}
	ref {
		name "hosts"
		path "org.zstack.header.vm.APIGetCandidateZonesClustersHostsForCreatingVmReply.hosts"
		desc "null"
		type "List"
		since "0.6"
		clz HostInventory.class
	}
	field {
		name "clusterPsMap"
		desc ""
		type "Map"
		since "0.6"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIGetCandidateZonesClustersHostsForCreatingVmReply.error"
		desc "null"
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
}
