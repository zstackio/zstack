package org.zstack.kvm.memory

doc {
	title "内存优化写回后端准备证据"

	field {
		name "candidateId"
		desc "由Host写回后端候选查询返回的设备候选标识；不是设备路径，格式为candidate:加64位小写十六进制摘要。"
		type "String"
		since "5.5.38"
	}
	field {
		name "expectedHostBootId"
		desc "候选查询时读取的Host启动标识；提交时必须仍与当前Host启动标识一致。"
		type "String"
		since "5.5.38"
	}
	field {
		name "expectedPoolGeneration"
		desc "候选查询时读取的当前ZRAM池代次；无现有池时为none，否则为64位小写十六进制摘要。"
		type "String"
		since "5.5.38"
	}
	field {
		name "backendCapacityBytes"
		desc "经候选查询确认的后端容量，单位字节；必须是4096的整数倍并处于接口允许范围内。"
		type "long"
		since "5.5.38"
	}
	field {
		name "resetConfirmed"
		desc "显式确认该候选设备可用于写回且其现有内容允许被重置；必须为true。"
		type "boolean"
		since "5.5.38"
	}
}
