package org.zstack.kvm.memory

import org.zstack.kvm.memory.MemoryVmExclusionInventory
import org.zstack.kvm.memory.MemoryFieldCapability

doc {

	title "内存优化策略"

	ref {
		name "migrationExclusion"
		path "org.zstack.kvm.memory.MemoryPolicyInventory.migrationExclusion"
		desc "VM迁移后保留的不参与状态证据；仅在存在该迁移记录时返回。"
		type "MemoryVmExclusionInventory"
		since "5.5.38"
		clz MemoryVmExclusionInventory.class
	}
	field {
		name "sourceRevisions"
		desc "各配置来源的revision快照，用于并发冲突检查。"
		type "Map"
		since "5.5.38"
	}
	field {
		name "fieldSources"
		desc "各字段当前生效值的配置来源。"
		type "Map"
		since "5.5.38"
	}
	field {
		name "fieldModes"
		desc "只读字段语义状态；ksm.enabled=Unmanaged 表示该作用域显式保留原生 KSM 管理，不下发启用/停用，也不继承父级该字段。"
		type "Map"
		since "5.5.38"
	}
	field {
		name "sourceRevision"
		desc "当前策略源的revision。"
		type "long"
		since "5.5.38"
	}
	field {
		name "allowedActions"
		desc "当前状态下可提交的动作提示；不是后续请求的授权保证。"
		type "List"
		since "5.5.38"
	}
	field {
		name "preflightRequiredActions"
		desc "执行某些动作前须先完成的预检动作。"
		type "List"
		since "5.5.38"
	}
	ref {
		name "fieldCapabilities"
		path "org.zstack.kvm.memory.MemoryPolicyInventory.fieldCapabilities"
		desc "字段能力映射；用于展示，不构成授权凭据。"
		type "Map"
		since "5.5.38"
		clz MemoryFieldCapability.class
	}
	field {
		name "source"
		desc "该策略值的来源粒度。"
		type "String"
		since "5.5.38"
	}
	field {
		name "scope"
		desc "策略资源粒度：Global、Cluster、Host 或 VM。"
		type "String"
		since "5.5.38"
	}
	field {
		name "resourceUuid"
		desc "资源UUID"
		type "String"
		since "5.5.38"
	}
	field {
		name "revision"
		desc "当前策略记录版本。"
		type "long"
		since "5.5.38"
	}
	field {
		name "policy"
		desc "该作用域显式配置的策略JSON；未设置的字段可继承上级值。"
		type "String"
		since "5.5.38"
	}
	field {
		name "effectivePolicy"
		desc "合并继承后用于执行的有效策略JSON。"
		type "String"
		since "5.5.38"
	}
	field {
		name "bootstrapStatus"
		desc "仅Global首次安装引导存在时返回的引导状态。"
		type "String"
		since "5.5.38"
	}
	field {
		name "bootstrapReason"
		desc "Global首次安装引导的阻塞或诊断原因。"
		type "String"
		since "5.5.38"
	}
}
