package org.zstack.network.service.lb

import org.zstack.network.service.lb.LoadBalancerListenerBackendServerInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询负载均衡监听器后端服务器状态回复"

	ref {
		name "inventories"
		path "org.zstack.network.service.lb.APIGetLoadBalancerListenerBackendServersReply.inventories"
		desc "后端服务器状态清单"
		type "List"
		since "5.5.38"
		clz LoadBalancerListenerBackendServerInventory.class
	}
	field {
		name "success"
		desc "操作是否成功"
		type "boolean"
		since "5.5.38"
	}
	ref {
		name "error"
		path "org.zstack.network.service.lb.APIGetLoadBalancerListenerBackendServersReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.5.38"
		clz ErrorCode.class
	}
}
