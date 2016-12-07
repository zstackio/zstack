package org.zstack.network.service.lb

import org.zstack.network.service.lb.LoadBalancerListenerInventory

doc {

	title "在这里输入结构的名称"

	field {
		name "name"
		desc "资源名称"
		type "String"
		since "0.6"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
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
		name "state"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "vipUuid"
		desc "VIP UUID"
		type "String"
		since "0.6"
	}
	ref {
		name "listeners"
		path "org.zstack.network.service.lb.LoadBalancerInventory.listeners"
		desc "null"
		type "List"
		since "0.6"
		clz LoadBalancerListenerInventory.class
	}
}
