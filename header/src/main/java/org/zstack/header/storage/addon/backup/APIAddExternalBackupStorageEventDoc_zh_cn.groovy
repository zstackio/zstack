package org.zstack.header.storage.addon.backup

import org.zstack.header.storage.addon.backup.ExternalBackupStorageInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "添加外部镜像存储结果"

	ref {
		name "inventory"
		path "org.zstack.header.storage.addon.backup.APIAddExternalBackupStorageEvent.inventory"
		desc "null"
		type "ExternalBackupStorageInventory"
		since "5.0.0"
		clz ExternalBackupStorageInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.0.0"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.addon.backup.APIAddExternalBackupStorageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.0.0"
		clz ErrorCode.class
	}
}
