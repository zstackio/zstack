package org.zstack.network.service.lb

import org.zstack.header.errorcode.ErrorCode
import org.zstack.network.service.lb.LoadBalancerListenerInventory

doc {

	title "负载均衡监听器清单"

	ref {
		name "error"
		path "org.zstack.network.service.lb.APICreateLoadBalancerListenerEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.network.service.lb.APICreateLoadBalancerListenerEvent.inventory"
		desc "null"
		type "LoadBalancerListenerInventory"
		since "0.6"
		clz LoadBalancerListenerInventory.class
	}
}
