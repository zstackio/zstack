package org.zstack.header.resource

import org.zstack.header.resource.ResourceSourceRefInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "QueryResourceSourceRefReply"

	ref {
		name "inventories"
		path "org.zstack.header.resource.APIQueryResourceSourceRefReply.inventories"
		desc "资源来源引用清单"
		type "List"
		since "5.5.28"
		clz ResourceSourceRefInventory.class
	}
	field {
		name "success"
		desc "API调用是否成功"
		type "boolean"
		since "5.5.28"
	}
	ref {
		name "error"
		path "org.zstack.header.resource.APIQueryResourceSourceRefReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.5.28"
		clz ErrorCode.class
	}
}
