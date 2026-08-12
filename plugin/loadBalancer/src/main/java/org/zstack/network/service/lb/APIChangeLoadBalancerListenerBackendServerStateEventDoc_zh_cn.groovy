package org.zstack.network.service.lb

import org.zstack.network.service.lb.LoadBalancerBackendServerStateResultInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "修改负载均衡监听器后端服务器状态事件"

	ref {
		name "results"
		path "org.zstack.network.service.lb.APIChangeLoadBalancerListenerBackendServerStateEvent.results"
		desc "各后端服务器的状态变更结果"
		type "List"
		since "5.5.38"
		clz LoadBalancerBackendServerStateResultInventory.class
	}
	field {
		name "success"
		desc "操作是否成功"
		type "boolean"
		since "5.5.38"
	}
	ref {
		name "error"
		path "org.zstack.network.service.lb.APIChangeLoadBalancerListenerBackendServerStateEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.5.38"
		clz ErrorCode.class
	}
}
