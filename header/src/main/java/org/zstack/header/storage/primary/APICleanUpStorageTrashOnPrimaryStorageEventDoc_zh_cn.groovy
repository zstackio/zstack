package org.zstack.header.storage.primary

import org.zstack.header.errorcode.ErrorCode

doc {

	title "清空主存储垃圾结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.primary.APICleanUpStorageTrashOnPrimaryStorageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	field {
		name "result"
		desc "清理成功的存储垃圾的集合"
		type "Map"
		since "4.8.0"
	}
	field {
		name "total"
		desc "清理成功的存储垃圾的数量"
		type "Integer"
		since "4.8.0"
	}
}
