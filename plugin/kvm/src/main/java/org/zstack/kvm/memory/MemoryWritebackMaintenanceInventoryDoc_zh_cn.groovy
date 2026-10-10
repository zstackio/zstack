package org.zstack.kvm.memory

doc {
    title "写回后端维护回执"
    field { name "schema"; desc "回执schema版本；缺失时为空。"; type "Long"; since "5.5.38" }
    field { name "requestId"; desc "维护请求ID。"; type "String"; since "5.5.38" }
    field { name "candidateId"; desc "请求绑定的候选设备身份。"; type "String"; since "5.5.38" }
    field { name "requestFingerprint"; desc "维护请求内容指纹。"; type "String"; since "5.5.38" }
    field { name "stage"; desc "持久维护状态阶段。"; type "String"; since "5.5.38" }
    field { name "reason"; desc "当前阶段原因；未提供时为null。"; type "String"; since "5.5.38" }
    field { name "startedAt"; desc "操作开始时间。"; type "String"; since "5.5.38" }
    field { name "updatedAt"; desc "回执最后更新时间。"; type "String"; since "5.5.38" }
    field { name "hostBootId"; desc "回执对应的Host启动ID。"; type "String"; since "5.5.38" }
    field { name "poolGeneration"; desc "回执对应的ZRAM池代次。"; type "String"; since "5.5.38" }
    ref { name "targetIdentity"; path "org.zstack.kvm.memory.MemoryWritebackMaintenanceInventory.targetIdentity"; desc "维护请求绑定的设备身份。"; type "MemoryWritebackMaintenanceTargetIdentityInventory"; since "5.5.38"; clz MemoryWritebackMaintenanceTargetIdentityInventory.class }
    field { name "resetAttempted"; desc "是否已经发起重置；缺失时为空。"; type "Boolean"; since "5.5.38" }
    field { name "archive"; desc "重置前状态归档位置。"; type "String"; since "5.5.38" }
    field { name "backendResourceUuid"; desc "维护后登记的后端资源UUID。"; type "String"; since "5.5.38" }
    field { name "request_id"; desc "已弃用的r12配置UI兼容别名；优先使用requestId。值为标识字符串，无数值单位；缺失时为空，与主字段保持一致。UI迁移及兼容期结束后移除。"; type "String"; since "5.5.38" }
    field { name "updated_at"; desc "已弃用的r12配置UI兼容别名；优先使用updatedAt。单位/格式与updatedAt相同（RFC3339时间）；缺失时为空，与主字段保持一致。UI迁移及兼容期结束后移除。"; type "String"; since "5.5.38" }
}
