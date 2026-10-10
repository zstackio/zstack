package org.zstack.kvm.memory

import org.zstack.kvm.memory.MemoryHostOperationsInventory.Entry
import org.zstack.kvm.memory.MemoryHostOperationsInventory.Concurrency
import org.zstack.kvm.memory.MemoryHostOperationsInventory.RecordBudget

doc {

	title "Host本地内存操作记录"

	field { name "hostUuid"; desc "查询结果所属物理机UUID。"; type "String"; since "5.5.38" }
	ref {
		name "entries"
		path "org.zstack.kvm.memory.MemoryHostOperationsInventory.entries"
		desc "Host本地操作结果记录；不等同于管理节点任务历史。"
		type "List"
		since "5.5.38"
		clz Entry.class
	}
	field { name "total"; desc "本次查询范围内的记录数。"; type "Long"; since "5.5.38" }
	field { name "nextPage"; desc "下一页偏移；为空表示没有后续页。"; type "Integer"; since "5.5.38" }
	ref {
		name "concurrency"
		path "org.zstack.kvm.memory.MemoryHostOperationsInventory.concurrency"
		desc "正常并发和超时隔离队列当前实际占用；不是配置上限。"
		type "Concurrency"
		since "5.5.38"
		clz Concurrency.class
	}
	ref {
		name "recordBudget"
		path "org.zstack.kvm.memory.MemoryHostOperationsInventory.recordBudget"
		desc "本地操作记录的容量统计；未启用容量跟踪时为空。"
		type "RecordBudget"
		since "5.5.38"
		clz RecordBudget.class
	}
}
