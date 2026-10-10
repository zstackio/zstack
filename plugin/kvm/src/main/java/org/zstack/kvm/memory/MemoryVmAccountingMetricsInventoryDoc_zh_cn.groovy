package org.zstack.kvm.memory

doc {

	title "VM ZRAM压缩指标"

	field { name "compressedPages"; desc "压缩页数；缺失时为空。"; type "Long"; since "5.5.38" }
	field { name "incompressiblePages"; desc "不可压缩页数；缺失时为空。"; type "Long"; since "5.5.38" }
	field { name "zeroPages"; desc "零页数；缺失时为空。"; type "Long"; since "5.5.38" }
	field { name "nonzeroSamePages"; desc "非零同值页数；缺失时为空。"; type "Long"; since "5.5.38" }
	field { name "backendCommittedPages"; desc "后端已承诺页数；缺失时为空。"; type "Long"; since "5.5.38" }
	field { name "ramPayloadBytes"; desc "驻留压缩负载字节数。"; type "Long"; since "5.5.38" }
	field { name "ramOriginalBytes"; desc "驻留数据压缩前逻辑字节数。"; type "Long"; since "5.5.38" }
	field { name "backendCommittedOriginalBytes"; desc "后端数据压缩前逻辑字节数；不是累计写入IO。"; type "Long"; since "5.5.38" }
	field { name "ramLogicalMinusPayloadBytes"; desc "驻留逻辑字节数减去压缩负载字节数；保留合法负值。"; type "Long"; since "5.5.38" }
	field { name "ramLogicalToPayloadRatio"; desc "驻留逻辑数据与压缩负载的比值；分母不可用时为空。"; type "Double"; since "5.5.38" }
	field { name "ratioState"; desc "比值状态，例如有效或不可计算。"; type "String"; since "5.5.38" }
	field { name "ram_payload_bytes"; desc "已弃用的r12 VM页面兼容别名；优先使用ramPayloadBytes。单位字节；未观测时为空，观测到0仍为0，与主字段保持一致。UI迁移及兼容期结束后移除。"; type "Long"; since "5.5.38" }
	field { name "ram_original_bytes"; desc "已弃用的r12 VM页面兼容别名；优先使用ramOriginalBytes。单位字节；未观测时为空，观测到0仍为0，与主字段保持一致。UI迁移及兼容期结束后移除。"; type "Long"; since "5.5.38" }
	field { name "backend_committed_original_bytes"; desc "已弃用的r12 VM页面兼容别名；优先使用backendCommittedOriginalBytes。单位字节；未观测时为空，观测到0仍为0，与主字段保持一致。UI迁移及兼容期结束后移除。"; type "Long"; since "5.5.38" }
}
