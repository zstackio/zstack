package org.zstack.header.host

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.host.HostInventory

doc {

	title "更新后的云主机"

	field {
		name "success"
		desc ""
		type "boolean"
		since "4.7.0"
	}
	ref {
		name "error"
		path "org.zstack.header.host.APIUpdateHostEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.7.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.host.APIUpdateHostEvent.inventory"
		desc "更新后的云主机信息"
		type "HostInventory"
		since "4.7.0"
		clz HostInventory.class
	}
}
