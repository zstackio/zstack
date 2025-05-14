package org.zstack.network.service.portforwarding

import org.zstack.network.service.portforwarding.PortForwardingRuleInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "Change port forwarding rule"

	ref {
		name "inventory"
		path "org.zstack.network.service.portforwarding.APIChangePortForwardingRuleEvent.inventory"
		desc "null"
		type "PortForwardingRuleInventory"
		since "5.4.0"
		clz PortForwardingRuleInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.4.0"
	}
	ref {
		name "error"
		path "org.zstack.network.service.portforwarding.APIChangePortForwardingRuleEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.4.0"
		clz ErrorCode.class
	}
}
