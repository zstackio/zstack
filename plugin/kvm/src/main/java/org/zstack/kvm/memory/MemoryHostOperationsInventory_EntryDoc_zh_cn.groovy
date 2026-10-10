package org.zstack.kvm.memory

doc {

	title "Host内存操作记录条目"

	field { name "operationId"; desc "操作幂等标识。"; type "String"; since "5.5.38" }
	field { name "vmUuid"; desc "目标VM UUID；writeback为池级操作时为空字符串。"; type "String"; since "5.5.38" }
	field { name "kind"; desc "操作类型，例如idle、writeback或reclaim。"; type "String"; since "5.5.38" }
	field { name "status"; desc "操作状态；保留服务返回的状态字符串。"; type "String"; since "5.5.38" }
	field { name "startedAt"; desc "操作开始时间，RFC3339格式。"; type "String"; since "5.5.38" }
	field { name "timeoutAt"; desc "操作超时时刻；未设置时为空。"; type "String"; since "5.5.38" }
	field { name "completedAt"; desc "操作完成时刻；尚未完成时为空。"; type "String"; since "5.5.38" }
	field { name "reason"; desc "操作结果原因；无原因时为空。"; type "String"; since "5.5.38" }
}
