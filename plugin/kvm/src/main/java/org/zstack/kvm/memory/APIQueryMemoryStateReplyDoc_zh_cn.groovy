package org.zstack.kvm.memory

import org.zstack.header.errorcode.ErrorCode
import org.zstack.kvm.memory.MemoryStateInventory
import java.lang.Integer

doc {

	title "Host内存优化状态分页响应"

	field {
		name "success"
		desc "API调用是否成功。"
		type "boolean"
		since "5.5.38"
	}
	ref {
		name "error"
		path "org.zstack.kvm.memory.APIQueryMemoryStateReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.5.38"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.kvm.memory.APIQueryMemoryStateReply.inventories"
		desc "当前页Host状态列表；指标内存量单位为bytes，采样时间为Unix epoch毫秒。"
		type "List"
		since "5.5.38"
		clz MemoryStateInventory.class
	}
	field {
		name "total"
		desc "满足过滤条件的记录总数。"
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
