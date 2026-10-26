package org.zstack.kvm.memory

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.longjob.LongJobInventory

doc {

	title "确认恢复的长任务"

	field {
		name "success"
		desc ""
		type "boolean"
		since "5.5.48"
	}
	ref {
		name "error"
		path "org.zstack.kvm.memory.APIRecoverHostMemoryOptimizationEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.5.48"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.kvm.memory.APIRecoverHostMemoryOptimizationEvent.inventory"
		desc "长任务信息"
		type "LongJobInventory"
		since "5.5.48"
		clz LongJobInventory.class
	}
}
