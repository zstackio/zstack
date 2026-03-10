package org.zstack.header.core.external.service

import org.zstack.header.core.external.service.ExternalServiceConfigurationInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "更新外部服务配置"

	ref {
		name "inventory"
		path "org.zstack.header.core.external.service.APIUpdateExternalServiceConfigurationEvent.inventory"
		desc "null"
		type "ExternalServiceConfigurationInventory"
		since "5.5.12"
		clz ExternalServiceConfigurationInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.5.12"
	}
	ref {
		name "error"
		path "org.zstack.header.core.external.service.APIUpdateExternalServiceConfigurationEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.5.12"
		clz ErrorCode.class
	}
}
