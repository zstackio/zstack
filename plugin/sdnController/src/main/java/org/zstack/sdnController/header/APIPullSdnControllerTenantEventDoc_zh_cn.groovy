package org.zstack.sdnController.header

import org.zstack.sdnController.header.H3cSdnControllerTenantInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "拉取SDN控制器租户结果"

	ref {
		name "inventories"
		path "org.zstack.sdnController.header.APIPullSdnControllerTenantEvent.inventories"
		desc "null"
		type "List"
		since "5.3.28"
		clz H3cSdnControllerTenantInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.3.28"
	}
	ref {
		name "error"
		path "org.zstack.sdnController.header.APIPullSdnControllerTenantEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.3.28"
		clz ErrorCode.class
	}
}
