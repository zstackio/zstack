package org.zstack.header.storage.backup

import org.zstack.header.errorcode.ErrorCode

doc {

	title "导出镜像清单"

	ref {
		name "error"
		path "org.zstack.header.storage.backup.APIExportImageFromBackupStorageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "1.7"
		clz ErrorCode.class
	}
	field {
		name "imageUrl"
		desc "被导出镜像的访问地址"
		type "String"
		since "1.7"
	}
	field {
		name "success"
		desc "导出成功失败标志"
		type "boolean"
		since "1.7"
	}
}
