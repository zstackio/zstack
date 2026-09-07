package org.zstack.network.service.lb

import org.zstack.header.errorcode.ErrorCode
import org.zstack.network.service.lb.LoadBalancerServerGroupBackendServerInventory

doc {

	title "查询负载均衡服务器组后端服务器列表返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.network.service.lb.APIGetLoadBalancerServerGroupBackendServerReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	field {
		name "total"
		desc "过滤后的后端服务器总数"
		type "Long"
		since "5.5.38"
	}
	ref {
		name "inventories"
		path "org.zstack.network.service.lb.APIGetLoadBalancerServerGroupBackendServerReply.inventories"
		desc "后端服务器列表"
		type "List"
		since "5.5.38"
		clz LoadBalancerServerGroupBackendServerInventory.class
	}
}
