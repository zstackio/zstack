package org.zstack.kvm.memory

import org.zstack.header.errorcode.ErrorCode
import org.zstack.kvm.memory.MemoryVmAccountingInventory

doc {

	title "批量VM内存优化统计响应"

	field {
		name "success"
		desc "API调用是否成功。"
		type "boolean"
		since "5.5.38"
	}
	ref {
		name "error"
		path "org.zstack.kvm.memory.APIGetVmMemoryOptimizationsReply.error"
		desc "错误码，若不为null，则表示操作失败；操作成功时该字段为null。",false
		type "ErrorCode"
		since "5.5.38"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.kvm.memory.APIGetVmMemoryOptimizationsReply.inventories"
		desc "按请求VM UUID索引的压缩记账结果；数值不可用时保留质量和原因，不用零替代。"
		type "Map"
		since "5.5.38"
		clz MemoryVmAccountingInventory.class
	}
}
