package org.zstack.storage.backup.sftp

import org.zstack.header.errorcode.ErrorCode

doc {

	title "Sftp镜像服务器清单"

	ref {
		name "error"
		path "org.zstack.storage.backup.sftp.APIAddSftpBackupStorageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.storage.backup.sftp.APIAddSftpBackupStorageEvent.inventory"
		desc "null"
		type "SftpBackupStorageInventory"
		since "0.6"
		clz SftpBackupStorageInventory.class
	}
}
