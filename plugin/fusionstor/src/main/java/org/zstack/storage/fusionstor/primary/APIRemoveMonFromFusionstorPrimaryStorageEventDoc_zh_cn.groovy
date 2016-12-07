package org.zstack.storage.fusionstor.primary

import org.zstack.header.errorcode.ErrorCode
import org.zstack.storage.fusionstor.primary.FusionstorPrimaryStorageInventory

doc {

	title "在这里输入结构的名称"

	ref {
		name "error"
		path "org.zstack.storage.fusionstor.primary.APIRemoveMonFromFusionstorPrimaryStorageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.storage.fusionstor.primary.APIRemoveMonFromFusionstorPrimaryStorageEvent.inventory"
		desc "null"
		type "FusionstorPrimaryStorageInventory"
		since "0.6"
		clz FusionstorPrimaryStorageInventory.class
	}
}
