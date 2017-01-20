package org.zstack.header.storage.backup

import org.zstack.header.errorcode.ErrorCode

doc {

	title "删除镜像服务器"

	ref {
		name "error"
		path "org.zstack.header.storage.backup.APIDeleteBackupStorageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
}
