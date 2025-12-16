package org.zstack.header.host

import org.zstack.header.host.HostInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "释放主机响应"

	ref {
		name "inventory"
		path "org.zstack.header.host.APIReleaseHostEvent.inventory"
		desc "主机清单信息"
		type "HostInventory"
		since "5.4.6"
		clz HostInventory.class
	}
	field {
		name "success"
		desc "操作是否成功"
		type "boolean"
		since "5.4.6"
	}
	ref {
		name "error"
		path "org.zstack.header.host.APIReleaseHostEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.4.6"
		clz ErrorCode.class
	}
}
