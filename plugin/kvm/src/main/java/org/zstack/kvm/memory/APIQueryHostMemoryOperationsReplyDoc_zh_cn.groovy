package org.zstack.kvm.memory

import org.zstack.header.errorcode.ErrorCode
import org.zstack.kvm.memory.MemoryHostOperationsInventory

doc {

	title "Host本地操作查询响应"

	field {
		name "success"
		desc "API调用是否成功。"
		type "boolean"
		since "5.5.38"
	}
	ref {
		name "error"
		path "org.zstack.kvm.memory.APIQueryHostMemoryOperationsReply.error"
		desc "错误码，若不为null，则表示操作失败；操作成功时该字段为null。",false
		type "ErrorCode"
		since "5.5.38"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.kvm.memory.APIQueryHostMemoryOperationsReply.inventory"
		desc "Host本地操作记录、并发额度和记录容量；不同于管理节点MemoryTask历史。"
		type "MemoryHostOperationsInventory"
		since "5.5.38"
		clz MemoryHostOperationsInventory.class
	}
}
