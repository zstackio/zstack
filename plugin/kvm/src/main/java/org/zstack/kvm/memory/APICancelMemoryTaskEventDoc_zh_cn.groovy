package org.zstack.kvm.memory

import org.zstack.header.errorcode.ErrorCode
import org.zstack.kvm.memory.MemoryTaskInventory

doc {

	title "取消内存优化任务事件"

	field {
		name "success"
		desc "API操作是否成功接受并处理。"
		type "boolean"
		since "5.5.38"
	}
	ref {
		name "error"
		path "org.zstack.kvm.memory.APICancelMemoryTaskEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.5.38"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.kvm.memory.APICancelMemoryTaskEvent.inventory"
		desc "取消后的任务记录；仅排队且尚未派发的任务可取消。"
		type "MemoryTaskInventory"
		since "5.5.38"
		clz MemoryTaskInventory.class
	}
}
