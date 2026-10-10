package org.zstack.kvm.memory

doc {
	title "内存优化不确定任务恢复证据"

	field {
		name "expectedHostBootId"
		desc "操作者核对的当前Host启动标识，必须与最新Host观测一致。"
		type "String"
		since "5.5.38"
	}
	field {
		name "expectedPoolGeneration"
		desc "操作者核对的待恢复旧ZRAM池代次，使用64位小写十六进制摘要。"
		type "String"
		since "5.5.38"
	}
	field {
		name "drainControlOperationUuid"
		desc "已成功完成且对应该Host/池代次的排空操作UUID，32位小写十六进制。"
		type "String"
		since "5.5.38"
	}
	field {
		name "confirmed"
		desc "显式确认恢复前置条件已由操作者核实；必须为true。恢复不会伪造原Unknown任务的成功或失败结果。"
		type "boolean"
		since "5.5.38"
	}
}
