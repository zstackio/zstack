package org.zstack.header.storage.primary

import org.zstack.header.storage.primary.PrimaryStorageInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "接管主存储返回"

	ref {
		name "inventory"
		path "org.zstack.header.storage.primary.APITakeoverPrimaryStorageEvent.inventory"
		desc "主存储信息"
		type "PrimaryStorageInventory"
		since "5.0.0"
		clz PrimaryStorageInventory.class
	}
	field {
		name "success"
		desc "操作是否成功"
		type "boolean"
		since "5.0.0"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.primary.APITakeoverPrimaryStorageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "5.0.0"
		clz ErrorCode.class
	}
	field {
		name "reconnectResult"
		desc "接管后重连结果，取值参见 ReconnectResult 枚举: SUCCESS（重连成功）/ FAILED（重连失败，但接管已完成且不可逆）/ NOT_ATTEMPTED（未尝试重连）"
		type "ReconnectResult"
		since "5.0.0"
	}
	field {
		name "reconnectError"
		desc "重连失败时的错误信息"
		type "String"
		since "5.0.0"
	}
}
