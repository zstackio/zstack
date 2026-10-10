package org.zstack.kvm.memory

import java.sql.Timestamp

doc {

	title "内存优化任务"

	field {
		name "expectedControlOperationUuid"
		desc "预期控制操作UUID，供Host端fencing核验。"
		type "String"
		since "5.5.38"
	}
	field {
		name "reconcileOperationUuid"
		desc "不确定操作恢复时关联的reconcile操作UUID。"
		type "String"
		since "5.5.38"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "5.5.38"
	}
	field {
		name "parentUuid"
		desc "任务父记录UUID；子Host任务指向批量任务。"
		type "String"
		since "5.5.38"
	}
	field {
		name "hostUuid"
		desc "物理机UUID"
		type "String"
		since "5.5.38"
	}
	field {
		name "scope"
		desc "策略作用域。"
		type "String"
		since "5.5.38"
	}
	field {
		name "resourceUuid"
		desc "任务目标资源UUID。"
		type "String"
		since "5.5.38"
	}
	field {
		name "action"
		desc "策略或维护动作名称。"
		type "String"
		since "5.5.38"
	}
	field {
		name "status"
		desc "任务执行状态；Succeeded只表示任务协议终态，Host结果应另行读取。"
		type "String"
		since "5.5.38"
	}
	field {
		name "reason"
		desc "执行结果或拒绝/失败原因；Unknown表示结果未能确定。"
		type "String"
		since "5.5.38"
	}
	field {
		name "desiredRevision"
		desc "任务创建时的目标策略revision。"
		type "long"
		since "5.5.38"
	}
	field {
		name "policyPlanHash"
		desc "策略计划摘要哈希。"
		type "String"
		since "5.5.38"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "5.5.38"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "5.5.38"
	}
}
