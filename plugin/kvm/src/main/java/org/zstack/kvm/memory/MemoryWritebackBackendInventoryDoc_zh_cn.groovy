package org.zstack.kvm.memory

doc {
    title "物理机写回盘清单"
    field { name "hostUuid"; desc "物理机UUID。"; type "String"; since "5.5.48" }
    field { name "status"; desc "Available：已获取到磁盘清单；Unsupported：拿不到磁盘清单（读不到开机ID、lsblk不可用或输出异常）；Disconnected：没有发现任何块设备。"; type "String"; since "5.5.48" }
    field { name "reasons"; desc "status不是Available的原因，或清单不完整的原因代码。"; type "List"; since "5.5.48" }
    field { name "observedAt"; desc "清单采集时间，毫秒时间戳。"; type "Long"; since "5.5.48" }
    field { name "preparationReasons"; desc "当前不能发起准备的原因代码；为空数组表示可以准备。"; type "List"; since "5.5.48" }
    ref { name "candidates"; path "org.zstack.kvm.memory.MemoryWritebackBackendInventory.candidates"; desc "可作为写回盘的盘，含已准备和正在使用的盘。"; type "List"; since "5.5.48"; clz MemoryWritebackBackendCandidateInventory.class }
}
