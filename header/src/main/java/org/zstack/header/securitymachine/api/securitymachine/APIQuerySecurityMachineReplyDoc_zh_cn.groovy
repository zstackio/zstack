package org.zstack.header.securitymachine.api.securitymachine

import org.zstack.header.securitymachine.SecurityMachineInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "在这里输入结构的名称"

	ref {
		name "inventories"
		path "org.zstack.header.securitymachine.securitymachine.api.APIQuerySecurityMachineReply.inventories"
		desc "null"
		type "List"
		since "0.6"
		clz SecurityMachineInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.securitymachine.securitymachine.api.APIQuerySecurityMachineReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
}
