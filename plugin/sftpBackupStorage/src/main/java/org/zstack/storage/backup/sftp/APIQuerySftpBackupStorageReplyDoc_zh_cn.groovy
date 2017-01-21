package org.zstack.storage.backup.sftp

import org.zstack.header.errorcode.ErrorCode

doc {

	title "Sftp镜像服务器清单"

	ref {
		name "error"
		path "org.zstack.storage.backup.sftp.APIQuerySftpBackupStorageReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.storage.backup.sftp.APIQuerySftpBackupStorageReply.inventories"
		desc "null"
		type "List"
		since "0.6"
		clz SftpBackupStorageInventory.class
	}
}
