package org.zstack.kvm.memory

import org.zstack.kvm.memory.MemoryVmAccountingMetricsInventory
import org.zstack.kvm.memory.MemoryVmIdentityInventory

doc {

	title "VM ZRAM压缩记账"

	field {
		name "hostUuid"
		desc "采集该VM记账数据的物理机UUID。"
		type "String"
		since "5.5.38"
	}
	field {
		name "vmUuid"
		desc "云主机UUID。"
		type "String"
		since "5.5.38"
	}
	field {
		name "quality"
		desc "数据质量；unavailable表示不能将缺失数据解释为零。"
		type "String"
		since "5.5.38"
	}
	field {
		name "reason"
		desc "数据不可用或不完整时的原因码；有效数据可为空。"
		type "String"
		since "5.5.38"
	}
	field {
		name "observedAt"
		desc "Host采样时间，RFC3339格式。"
		type "String"
		since "5.5.38"
	}
	field {
		name "hostBootId"
		desc "Host启动标识，用于区分重启前后的采样。"
		type "String"
		since "5.5.38"
	}
	field {
		name "device"
		desc "ZRAM设备标识。"
		type "String"
		since "5.5.38"
	}
	field {
		name "ownershipSemantics"
		desc "per-VM记账的归属语义。"
		type "String"
		since "5.5.38"
	}
	field {
		name "displayTtlMillis"
		desc "该样本允许用于展示的有效期，单位毫秒。"
		type "Long"
		since "5.5.38"
	}
	field {
		name "schemaVersion"
		desc "Agent记账数据结构版本。"
		type "Integer"
		since "5.5.38"
	}
	field {
		name "poolGeneration"
		desc "ZRAM池代次标识。"
		type "String"
		since "5.5.38"
	}
	field {
		name "sequence"
		desc "样本序列号；以字符串保留无符号整数精度。"
		type "String"
		since "5.5.38"
	}
	ref {
		name "identity"
		path "org.zstack.kvm.memory.MemoryVmAccountingInventory.identity"
		desc "Agent提供的VM及cgroup身份信息；为兼容既有字段保留。"
		type "MemoryVmIdentityInventory"
		since "5.5.38"
		clz MemoryVmIdentityInventory.class
	}
	ref {
		name "metrics"
		path "org.zstack.kvm.memory.MemoryVmAccountingInventory.metrics"
		desc "per-VM ZRAM压缩记账指标；字段单位见指标定义，缺失值为空而非零。"
		type "MemoryVmAccountingMetricsInventory"
		since "5.5.38"
		clz MemoryVmAccountingMetricsInventory.class
	}
	field { name "observed_at"; desc "已弃用的r12列表UI迁移兼容别名；优先使用observedAt。单位/格式与observedAt相同（RFC3339采样时间）；未观测时为空，与主字段保持一致。UI迁移及兼容期结束后移除。"; type "String"; since "5.5.38" }
}
