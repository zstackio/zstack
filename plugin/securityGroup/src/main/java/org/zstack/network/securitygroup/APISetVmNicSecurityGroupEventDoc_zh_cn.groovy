package org.zstack.network.securitygroup

import org.zstack.network.securitygroup.VmNicSecurityGroupRefInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "安全组（Security Group）清单"

	ref {
		name "inventory"
		path "org.zstack.network.securitygroup.APISetVmNicSecurityGroupEvent.inventory"
		desc "null"
		type "List"
		since "4.7.21"
		clz VmNicSecurityGroupRefInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.7.21"
	}
	ref {
		name "error"
		path "org.zstack.network.securitygroup.APISetVmNicSecurityGroupEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.7.21"
		clz ErrorCode.class
	}
}
