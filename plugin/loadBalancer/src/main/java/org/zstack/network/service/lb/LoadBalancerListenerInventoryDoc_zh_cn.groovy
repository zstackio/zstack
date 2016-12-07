package org.zstack.network.service.lb

import java.lang.Integer
import java.lang.Integer
import java.sql.Timestamp
import java.sql.Timestamp
import org.zstack.network.service.lb.LoadBalancerListenerVmNicRefInventory

doc {

	title "在这里输入结构的名称"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "0.6"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "0.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "0.6"
	}
	field {
		name "loadBalancerUuid"
		desc "负载均衡器UUID"
		type "String"
		since "0.6"
	}
	field {
		name "instancePort"
		desc ""
		type "Integer"
		since "0.6"
	}
	field {
		name "loadBalancerPort"
		desc ""
		type "Integer"
		since "0.6"
	}
	field {
		name "protocol"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "0.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "0.6"
	}
	ref {
		name "vmNicRefs"
		path "org.zstack.network.service.lb.LoadBalancerListenerInventory.vmNicRefs"
		desc "null"
		type "List"
		since "0.6"
		clz LoadBalancerListenerVmNicRefInventory.class
	}
}
