package org.zstack.kvm.memory

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.longjob.LongJobInventory

doc {

	title "准备写回后端的长任务"

	field {
		name "success"
		desc "准备请求是否已作为长任务提交；写回盘是否准备完成见长任务结果"
		type "boolean"
		since "5.5.48"
	}
	ref {
		name "error"
		path "org.zstack.kvm.memory.APIPrepareHostMemoryWritebackBackendEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.5.48"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.kvm.memory.APIPrepareHostMemoryWritebackBackendEvent.inventory"
		desc "长任务信息"
		type "LongJobInventory"
		since "5.5.48"
		clz LongJobInventory.class
	}
}
