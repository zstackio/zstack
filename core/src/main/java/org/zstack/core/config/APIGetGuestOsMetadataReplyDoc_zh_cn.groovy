package org.zstack.core.config

import org.zstack.core.config.GuestOsCharacterInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取虚拟机操作系统元数据的返回"

	ref {
		name "inventories"
		path "org.zstack.core.config.APIGetGuestOsMetadataReply.inventories"
		desc "null"
		type "List"
		since "5.1.0"
		clz GuestOsCharacterInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.1.0"
	}
	ref {
		name "error"
		path "org.zstack.core.config.APIGetGuestOsMetadataReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.1.0"
		clz ErrorCode.class
	}
}
