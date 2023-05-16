package org.zstack.header.host

import org.zstack.header.host.HostInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "物理机开机消息回复"

	ref {
		name "inventory"
		path "org.zstack.header.host.APIPowerOnHostEvent.inventory"
		desc "null"
		type "HostInventory"
		since "0.6"
		clz HostInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.host.APIPowerOnHostEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
}
