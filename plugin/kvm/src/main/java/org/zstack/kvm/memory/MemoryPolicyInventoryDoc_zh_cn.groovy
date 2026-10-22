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
		desc "各字段合成期望值的来源；四功能开关按Host、Cluster、Global取最近明确布尔值的层，none向上继承，无有效布尔值时没有来源；其它参数取Host、Cluster、Global最近明确值。不证明Host已执行，也不表示该层之外没有本级配置行。"
		type "Map"
		since "5.5.38"
	}
	field {
		name "fieldModes"
		desc "只读字段语义状态；四功能开关的Unmanaged表示三级均无有效布尔值，该开关不下发启用/停用并保留原生状态；本级none仍向上继承。"
		type "Map"
		since "5.5.38"
	}
	field {
		name "sourceRevision"
		desc "当前策略源的revision。"
		type "long"
		since "5.5.38"
	}
	ref {
		name "fieldCapabilities"
		path "org.zstack.kvm.memory.MemoryPolicyInventory.fieldCapabilities"
		desc "字段配置资格及原因提示，不构成授权凭据；writeback开关可保存合法期望值与Host当前能否执行须区分，后端字段仅Host。"
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
		desc "该作用域的配置JSON；本级false仍保留false，功能开关的跨层合成结果另见effectivePolicy，不能用生效来源判断本级是否有记录。"
		type "String"
		since "5.5.38"
	}
	field {
		name "effectivePolicy"
		desc "合成后的期望策略JSON；KSM/零页/ZRAM/writeback四开关取Host、Cluster、Global最近明确布尔值，false覆盖上级true，none向上继承，三级无有效值不管理；其它参数取最近明确值。writeback可能因Host后端未准备或能力/保护暂未执行，实际生效须另查Host状态。"
		type "String"
		since "5.5.38"
	}
}
