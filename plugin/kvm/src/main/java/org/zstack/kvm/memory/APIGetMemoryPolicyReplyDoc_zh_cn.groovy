package org.zstack.kvm.memory

import org.zstack.header.errorcode.ErrorCode
import org.zstack.kvm.memory.MemoryPolicyInventory

doc {

	title "读取内存优化策略响应"

	field {
		name "success"
		desc "API调用是否成功。"
		type "boolean"
		since "5.5.38"
	}
	ref {
		name "error"
		path "org.zstack.kvm.memory.APIGetMemoryPolicyReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.5.38"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.kvm.memory.APIGetMemoryPolicyReply.inventory"
		desc "策略inventory，包含revision、继承来源、能力及有效策略。"
		type "MemoryPolicyInventory"
		since "5.5.38"
		clz MemoryPolicyInventory.class
	}
}
