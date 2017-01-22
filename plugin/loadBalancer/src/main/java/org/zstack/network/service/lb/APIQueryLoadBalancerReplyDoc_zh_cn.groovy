package org.zstack.network.service.lb

import org.zstack.header.errorcode.ErrorCode
import org.zstack.network.service.lb.LoadBalancerInventory

doc {

	title "负载均衡器清单"

	ref {
		name "error"
		path "org.zstack.network.service.lb.APIQueryLoadBalancerReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.network.service.lb.APIQueryLoadBalancerReply.inventories"
		desc "null"
		type "List"
		since "0.6"
		clz LoadBalancerInventory.class
	}
}
