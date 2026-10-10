package org.zstack.kvm.memory

doc {
    title "写回后端候选设备"
    field { name "identity"; desc "本次候选标识；未准备候选使用candidate:前缀。"; type "String"; since "5.5.38" }
    field { name "fingerprint"; desc "候选稳定指纹。"; type "String"; since "5.5.38" }
    field { name "resourceUuid"; desc "服务已登记的后端资源UUID；未登记时为null。"; type "String"; since "5.5.38" }
    field { name "type"; desc "设备类型。"; type "String"; since "5.5.38" }
    field { name "path"; desc "候选设备路径或稳定by-id路径。"; type "String"; since "5.5.38" }
    field { name "device"; desc "规范化设备路径。"; type "String"; since "5.5.38" }
    field { name "byIdPath"; desc "稳定设备别名；未发现时为空字符串。"; type "String"; since "5.5.38" }
    field { name "capacityBytes"; desc "设备容量，单位字节；上游未提供时为空。"; type "Long"; since "5.5.38" }
    field { name "hostBootId"; desc "候选设备采集时的Host启动ID。"; type "String"; since "5.5.38" }
    field { name "eligible"; desc "设备是否通过资格筛选；上游未提供时为空，不代表未通过。"; type "Boolean"; since "5.5.38" }
    field { name "writebackReady"; desc "服务是否已完成该后端的准备；上游未提供时为空，不代表未准备。"; type "Boolean"; since "5.5.38" }
    field { name "state"; desc "设备准备或资格状态。"; type "String"; since "5.5.38" }
    field { name "reasons"; desc "不符合资格的原因代码。"; type "List"; since "5.5.38" }
    field { name "transport"; desc "设备传输类型。"; type "String"; since "5.5.38" }
    ref { name "stableIdentity"; path "org.zstack.kvm.memory.MemoryWritebackBackendCandidateInventory.stableIdentity"; desc "用于复核设备身份的稳定字段。"; type "MemoryWritebackStableIdentityInventory"; since "5.5.38"; clz MemoryWritebackStableIdentityInventory.class }
    field { name "active"; desc "服务是否正在使用该后端；上游未提供时为空。"; type "Boolean"; since "5.5.38" }
}
