package org.zstack.kvm.memory

import org.zstack.header.errorcode.ErrorCode
import org.zstack.kvm.memory.MemoryTaskInventory

doc {

	title "更新内存策略事件"

	field {
		name "success"
		desc "API请求是否成功创建/处理；异步接受不代表Host已应用。"
		type "boolean"
		since "5.5.38"
	}
	ref {
		name "error"
		path "org.zstack.kvm.memory.APIUpdateMemoryPolicyEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.5.38"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.kvm.memory.APIUpdateMemoryPolicyEvent.inventory"
		desc "关联内存任务；继续查询任务并读取Host状态确认执行结果。"
		type "MemoryTaskInventory"
		since "5.5.38"
		clz MemoryTaskInventory.class
	}
}
