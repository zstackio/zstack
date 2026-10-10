package org.zstack.kvm.memory

import org.zstack.header.errorcode.ErrorCode
import org.zstack.kvm.memory.MemorySummaryInventory

doc {

	title "内存优化汇总响应"

	field {
		name "success"
		desc "API调用是否成功。"
		type "boolean"
		since "5.5.38"
	}
	ref {
		name "error"
		path "org.zstack.kvm.memory.APIGetMemorySummaryReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.5.38"
		clz ErrorCode.class
	}
	ref {
		name "summary"
		path "org.zstack.kvm.memory.APIGetMemorySummaryReply.summary"
		desc "汇总节省量、样本质量及覆盖信息；字节字段单位为bytes。"
		type "MemorySummaryInventory"
		since "5.5.38"
		clz MemorySummaryInventory.class
	}
}
