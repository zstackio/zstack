package org.zstack.network.service.lb

import org.zstack.network.service.lb.LoadBalancerInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "在这里输入结构的名称"

	ref {
		name "inventory"
		path "org.zstack.network.service.lb.APIAttachVipToLoadBalancerEvent.inventory"
		desc "null"
		type "LoadBalancerInventory"
		since "4.8.0"
		clz LoadBalancerInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.8.0"
	}
	ref {
		name "error"
		path "org.zstack.network.service.lb.APIAttachVipToLoadBalancerEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.8.0"
		clz ErrorCode.class
	}
}
