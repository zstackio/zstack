package org.zstack.header.storage.primary

import org.zstack.header.core.trash.TrashCleanupResult
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.core.trash.CleanTrashResult

doc {

	title "清理主存储上的回收数据结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.primary.APICleanUpTrashOnPrimaryStorageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.2.0"
		clz ErrorCode.class
	}
	ref {
		name "result"
		path "org.zstack.header.storage.primary.APICleanUpTrashOnPrimaryStorageEvent.result"
		desc "清理数据返回信息"
		type "CleanTrashResult"
		since "3.3.0"
		clz CleanTrashResult.class
	}
	ref {
		name "results"
		path "org.zstack.header.storage.primary.APICleanUpTrashOnPrimaryStorageEvent.results"
		desc "清理数据返回信息"
		type "List"
		since "4.7.0"
		clz TrashCleanupResult.class
	}
}
