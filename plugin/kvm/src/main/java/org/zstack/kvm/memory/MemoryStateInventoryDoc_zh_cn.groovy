package org.zstack.kvm.memory

import java.lang.Boolean
import java.lang.Long

doc {

	title "Host内存优化状态"

	field {
		name "schemaVersion"
		desc "状态数据结构版本。"
		type "String"
		since "5.5.38"
	}
	field {
		name "bootId"
		desc "Host启动标识；用于识别重启前后的状态边界。"
		type "String"
		since "5.5.38"
	}
	field {
		name "featureState"
		desc "Agent报告的功能运行状态。"
		type "String"
		since "5.5.38"
	}
	field {
		name "featureStateSource"
		desc "功能状态的来源。"
		type "String"
		since "5.5.38"
	}
	field {
		name "drift"
		desc "当前状态与期望配置是否存在漂移；未知时为null。"
		type "Boolean"
		since "5.5.38"
	}
	field {
		name "dataAgeSeconds"
		desc "观测数据年龄，单位秒。"
		type "Long"
		since "5.5.38"
	}
	field {
		name "quality"
		desc "数据质量状态；不合格样本不可解释为零。"
		type "String"
		since "5.5.38"
	}
	field {
		name "metrics"
		desc "原生指标映射；内存量值单位为字节。"
		type "Map"
		since "5.5.38"
	}
	field {
		name "allowedActions"
		desc "当前建议动作；仅作状态提示。"
		type "List"
		since "5.5.38"
	}
	field {
		name "controlOperationUuid"
		desc "当前Host控制操作UUID。"
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
		name "desiredRevision"
		desc "Host期望策略revision。"
		type "long"
		since "5.5.38"
	}
	field {
		name "appliedRevision"
		desc "最后已读回的应用revision；尚未应用时为null。"
		type "Long"
		since "5.5.38"
	}
	field {
		name "status"
		desc "控制器记录的应用状态。"
		type "String"
		since "5.5.38"
	}
	field {
		name "reason"
		desc "状态原因或失败诊断；Unknown不会被折叠为成功。"
		type "String"
		since "5.5.38"
	}
	field {
		name "capabilities"
		desc "Host能力JSON快照。"
		type "String"
		since "5.5.38"
	}
	field {
		name "state"
		desc "Agent报告的详细运行状态。"
		type "String"
		since "5.5.38"
	}
	field {
		name "lastSampleTime"
		desc "最近一次采样时间，Unix epoch毫秒。"
		type "Long"
		since "5.5.38"
	}
	field {
		name "activeTaskUuid"
		desc "当前活动任务UUID；无活动任务时为null。"
		type "String"
		since "5.5.38"
	}
	field {
		name "appliedPolicyHash"
		desc "已应用策略摘要哈希。"
		type "String"
		since "5.5.38"
	}
	field {
		name "appliedPolicy"
		desc "最近成功读回的应用策略JSON。"
		type "String"
		since "5.5.38"
	}
	field {
		name "policyTargetHash"
		desc "目标策略摘要哈希。"
		type "String"
		since "5.5.38"
	}
	field {
		name "policyPlanHash"
		desc "策略计划摘要哈希。"
		type "String"
		since "5.5.38"
	}
	field {
		name "policyPlanStatus"
		desc "策略计划状态；需要复核不等同于已应用。"
		type "String"
		since "5.5.38"
	}
	field {
		name "policyBlockedFields"
		desc "策略阻塞字段及结构化原因JSON。"
		type "String"
		since "5.5.38"
	}
}
