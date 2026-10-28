package org.zstack.kvm.memory

doc {
    title "Host写回后端清单"
    field { name "hostUuid"; desc "清单对应的物理机UUID。"; type "String"; since "5.5.48" }
    field { name "bootId"; desc "采集时的Host启动ID。"; type "String"; since "5.5.48" }
    field { name "observedAt"; desc "候选设备清单的采集时间。"; type "String"; since "5.5.48" }
    field { name "status"; desc "设备发现状态。"; type "String"; since "5.5.48" }
    field { name "reasons"; desc "清单状态原因代码。"; type "List"; since "5.5.48" }
    field { name "candidatesFieldPresent"; desc "上游是否提供了candidates字段；本字段始终返回true或false，与candidates=null及空数组区分。"; type "Boolean"; since "5.5.48" }
    ref { name "candidates"; path "org.zstack.kvm.memory.MemoryWritebackBackendInventory.candidates"; desc "发现的候选设备；null、缺失和空数组保持各自语义。"; type "List"; since "5.5.48"; clz MemoryWritebackBackendCandidateInventory.class }
    field { name "controlOperationUuid"; desc "Agent候选观测的控制操作UUID，未确认时为null；它不是管理端States控制身份，不能作为prepareWritebackBackend请求的expectedControlOperationUuid。"; type "String"; since "5.5.48" }
    ref { name "maintenance"; path "org.zstack.kvm.memory.MemoryWritebackBackendInventory.maintenance"; desc "最近一次后端维护回执的公开字段。"; type "MemoryWritebackMaintenanceInventory"; since "5.5.48"; clz MemoryWritebackMaintenanceInventory.class }
    field { name "poolGeneration"; desc "当前ZRAM池代次或显式none；它是opaque token，不是设备UUID。"; type "String"; since "5.5.48" }
    field { name "canPrepare"; desc "是否已具备直接离线准备候选后端的资格；未提供观测时为空，不代表false。已有池仅因服务未停或缺排空证明而为false时，自动维护仍须核完整排空条件。首次无池且唯一阻碍为服务未停时，后端核无数据、无未决及服务身份后自动正常退出、准备及恢复，超时不强杀；不要求前端先drain或手动停服务。"; type "Boolean"; since "5.5.48" }
    field { name "preparationReasons"; desc "直接离线准备的资格原因代码；自动维护不得忽略其他拒绝原因，false却没有原因属于未知资格。"; type "List"; since "5.5.48" }
}
