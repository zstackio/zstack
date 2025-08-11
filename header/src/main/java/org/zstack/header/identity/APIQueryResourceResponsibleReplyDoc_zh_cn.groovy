package org.zstack.header.identity

import org.zstack.header.identity.ResourceResponsibleInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询资源责任关系返回"

	ref {
		name "inventories"
		path "org.zstack.header.identity.APIQueryResourceResponsibleReply.inventories"
		desc "null"
		type "List"
		since "0.6"
		clz ResourceResponsibleInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.identity.APIQueryResourceResponsibleReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
}
