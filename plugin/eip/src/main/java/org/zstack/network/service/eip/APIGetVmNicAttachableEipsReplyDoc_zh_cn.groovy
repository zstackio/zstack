package org.zstack.network.service.eip

import org.zstack.network.service.eip.EipInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取云主机网卡可挂载弹性IP的返回"

	ref {
		name "inventories"
		path "org.zstack.network.service.eip.APIGetVmNicAttachableEipsReply.inventories"
		desc "null"
		type "List"
		since "4.3.18"
		clz EipInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.3.18"
	}
	ref {
		name "error"
		path "org.zstack.network.service.eip.APIGetVmNicAttachableEipsReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.3.18"
		clz ErrorCode.class
	}
}
