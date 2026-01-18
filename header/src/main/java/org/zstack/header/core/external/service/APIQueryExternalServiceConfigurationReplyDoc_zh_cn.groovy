package org.zstack.header.core.external.service

import org.zstack.header.core.external.service.ExternalServiceConfigurationInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "在这里输入结构的名称"

	ref {
		name "inventories"
		path "org.zstack.header.core.external.service.APIQueryExternalServiceConfigurationReply.inventories"
		desc "null"
		type "List"
		since "5.4.6"
		clz ExternalServiceConfigurationInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.4.6"
	}
	ref {
		name "error"
		path "org.zstack.header.core.external.service.APIQueryExternalServiceConfigurationReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.4.6"
		clz ErrorCode.class
	}
}
