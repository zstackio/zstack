package org.zstack.header.host

import org.zstack.header.host.HostNetworkLabelInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "在这里输入结构的名称"

	ref {
		name "inventory"
		path "org.zstack.header.host.APICreateHostNetworkServiceTypeEvent.inventory"
		desc "null"
		type "HostNetworkLabelInventory"
		since "5.2.1"
		clz HostNetworkLabelInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.2.1"
	}
	ref {
		name "error"
		path "org.zstack.header.host.APICreateHostNetworkServiceTypeEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.2.1"
		clz ErrorCode.class
	}
}
