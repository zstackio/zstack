package org.zstack.header.host

import java.lang.Long
import java.lang.Long

doc {

	title "物理机NUMA Node"

	field {
		name "distance"
		desc "NUMA node 距离"
		type "List"
		since "4.3.12"
	}
	field {
		name "cpus"
		desc "NUMA node所有的CPU列表"
		type "List"
		since "0.6"
	}
	field {
		name "free"
		desc "NUMA node可用内存大小(B)"
		type "Long"
		since "0.6"
	}
	field {
		name "size"
		desc "NUMA node内存大小(B)"
		type "Long"
		since "0.6"
	}
	field {
		name "nodeID"
		desc "NUMA node ID"
		type "String"
		since "0.6"
	}
	field {
		name "VMsUuid"
		desc "关联NUMA node的云主机ID列表"
		type "List"
		since "0.6"
	}
}
