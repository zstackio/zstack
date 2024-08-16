package org.zstack.header.storage.addon.primary

import org.zstack.header.errorcode.ErrorCode

doc {

	title "发现外部存储"

	ref {
		name "inventory"
		path "org.zstack.header.storage.addon.primary.APIDiscoverExternalPrimaryStorageEvent.inventory"
		desc "null"
		type "ExternalPrimaryStorageInventory"
		since "5.0.0"
		clz ExternalPrimaryStorageInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.0.0"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.addon.primary.APIDiscoverExternalPrimaryStorageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.0.0"
		clz ErrorCode.class
	}
}
