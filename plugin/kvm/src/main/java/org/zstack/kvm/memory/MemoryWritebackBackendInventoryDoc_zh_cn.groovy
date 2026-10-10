package org.zstack.kvm.memory

doc {
    title "Host写回后端清单"
    field { name "hostUuid"; desc "清单对应的物理机UUID。"; type "String"; since "5.5.38" }
    field { name "bootId"; desc "采集时的Host启动ID。"; type "String"; since "5.5.38" }
    field { name "observedAt"; desc "候选设备清单的采集时间。"; type "String"; since "5.5.38" }
    field { name "status"; desc "设备发现状态。"; type "String"; since "5.5.38" }
    field { name "reasons"; desc "清单状态原因代码。"; type "List"; since "5.5.38" }
    field { name "candidatesFieldPresent"; desc "上游是否提供了candidates字段；本字段始终返回true或false，与candidates=null及空数组区分。"; type "Boolean"; since "5.5.38" }
    ref { name "candidates"; path "org.zstack.kvm.memory.MemoryWritebackBackendInventory.candidates"; desc "发现的候选设备；null、缺失和空数组保持各自语义。"; type "List"; since "5.5.38"; clz MemoryWritebackBackendCandidateInventory.class }
    field { name "controlOperationUuid"; desc "当前控制操作UUID，未确认时为null。"; type "String"; since "5.5.38" }
    ref { name "maintenance"; path "org.zstack.kvm.memory.MemoryWritebackBackendInventory.maintenance"; desc "最近一次后端维护回执的公开字段。"; type "MemoryWritebackMaintenanceInventory"; since "5.5.38"; clz MemoryWritebackMaintenanceInventory.class }
    field { name "poolGeneration"; desc "当前ZRAM池代次或显式none；它是opaque token，不是设备UUID。"; type "String"; since "5.5.38" }
    field { name "canPrepare"; desc "是否允许准备已筛选候选后端；未提供资格观测时为空，不代表false。"; type "Boolean"; since "5.5.38" }
    field { name "preparationReasons"; desc "后端准备资格原因代码。"; type "List"; since "5.5.38" }
    field { name "canPrepareZramPool"; desc "是否允许重新准备ZRAM池；未提供资格观测时为空，不代表false。"; type "Boolean"; since "5.5.38" }
    field { name "zramPoolPreparationReasons"; desc "ZRAM池准备资格原因代码。"; type "List"; since "5.5.38" }
    field { name "zramPoolPreparationQuality"; desc "ZRAM池准备资格观测质量。"; type "String"; since "5.5.38" }
    field { name "zramPoolPreparationObservedAt"; desc "ZRAM池准备资格采样时间，单位毫秒；未提供采样时为空。"; type "Long"; since "5.5.38" }
}
