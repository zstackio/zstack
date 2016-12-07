package org.zstack.header.host

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.host.HostInventory

doc {

	title "在这里输入结构的名称"

	ref {
		name "error"
		path "org.zstack.header.host.APIReconnectHostEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.host.APIReconnectHostEvent.inventory"
		desc "null"
		type "HostInventory"
		since "0.6"
		clz HostInventory.class
	}
}
