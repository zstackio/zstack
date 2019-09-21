package org.zstack.header.storage.backup

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取备份存储上的回收数据列表"

	ref {
		name "error"
		path "org.zstack.header.storage.backup.APIGetTrashOnBackupStorageReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.2.0"
		clz ErrorCode.class
	}
	ref {
		name "storageTrashSpecs"
		path "org.zstack.header.storage.backup.APIGetTrashOnBackupStorageReply.storageTrashSpecs"
		desc "回收数据清单"
		type "List"
		since "3.2.0"
		clz StorageTrashSpec.class
	}
}
