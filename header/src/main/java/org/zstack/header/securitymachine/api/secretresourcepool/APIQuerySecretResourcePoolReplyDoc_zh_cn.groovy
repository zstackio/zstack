package org.zstack.header.securitymachine.api.secretresourcepool

import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "在这里输入结构的名称"

	ref {
		name "inventories"
		path "org.zstack.header.securitymachine.secretresourcepool.api.APIQuerySecretResourcePoolReply.inventories"
		desc "null"
		type "List"
		since "0.6"
		clz SecretResourcePoolInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.securitymachine.secretresourcepool.api.APIQuerySecretResourcePoolReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
}
