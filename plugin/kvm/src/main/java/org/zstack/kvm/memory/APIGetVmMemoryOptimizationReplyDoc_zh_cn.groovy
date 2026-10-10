package org.zstack.kvm.memory

import org.zstack.header.errorcode.ErrorCode
import org.zstack.kvm.memory.MemoryVmAccountingInventory

doc {

	title "VM内存优化统计响应"

	field {
		name "success"
		desc "API调用是否成功。"
		type "boolean"
		since "5.5.38"
	}
	ref {
		name "error"
		path "org.zstack.kvm.memory.APIGetVmMemoryOptimizationReply.error"
		desc "错误码，若不为null，则表示操作失败；操作成功时该字段为null。",false
		type "ErrorCode"
		since "5.5.38"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.kvm.memory.APIGetVmMemoryOptimizationReply.inventory"
		desc "单VM压缩记账、指标及数据质量；内存量单位为字节。"
		type "MemoryVmAccountingInventory"
		since "5.5.38"
		clz MemoryVmAccountingInventory.class
	}
}
