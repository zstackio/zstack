package org.zstack.kvm.memory

import org.zstack.header.errorcode.ErrorCode
import org.zstack.kvm.memory.MemoryTaskInventory
import java.lang.Integer

doc {

	title "内存优化任务分页响应"

	field {
		name "success"
		desc "API调用是否成功。"
		type "boolean"
		since "5.5.38"
	}
	ref {
		name "error"
		path "org.zstack.kvm.memory.APIQueryMemoryTaskReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.5.38"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.kvm.memory.APIQueryMemoryTaskReply.inventories"
		desc "当前页管理节点任务记录；任务状态不取代Host原生结果读回。"
		type "List"
		since "5.5.38"
		clz MemoryTaskInventory.class
	}
	field {
		name "total"
		desc "满足过滤条件的任务总数。"
		type "long"
		since "5.5.38"
	}
	field {
		name "nextPage"
		desc "下一页start偏移；为null时表示没有后续页。"
		type "Integer"
		since "5.5.38"
	}
	field {
		name "snapshotId"
		desc "本次查询快照令牌；请求下一页时原样传回。"
		type "String"
		since "5.5.38"
	}
}
