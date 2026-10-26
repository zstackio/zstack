package org.zstack.kvm.memory

import org.zstack.kvm.memory.HostMemoryStateInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "物理机内存优化状态查询结果"

	ref {
		name "inventories"
		path "org.zstack.kvm.memory.APIQueryHostMemoryStateReply.inventories"
		desc "物理机内存优化状态列表"
		type "List"
		since "5.5.48"
		clz HostMemoryStateInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.5.48"
	}
	ref {
		name "error"
		path "org.zstack.kvm.memory.APIQueryHostMemoryStateReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.5.48"
		clz ErrorCode.class
	}
}
