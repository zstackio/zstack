package org.zstack.kvm.memory

doc {
    title "物理机交换盘"
    field { name "hostUuid"; desc "物理机UUID。"; type "String"; since "5.7.10" }
    field { name "status"; desc "NotConfigured：未配置交换盘；Active：交换盘已启用；Fault：交换盘找不到、被更换、未启用或准备中断，不会自动重新格式化，可停用后重新准备。"; type "String"; since "5.7.10" }
    ref { name "swapDisk"; path "org.zstack.kvm.memory.MemorySwapDiskInventory.swapDisk"; desc "已配置的交换盘；未配置时为null。"; type "MemorySwapDiskDeviceInventory"; since "5.7.10"; clz MemorySwapDiskDeviceInventory.class }
    field { name "otherSwaps"; desc "交换盘之外正在使用的交换设备，例如内存压缩设备；存在时不能准备交换盘。"; type "List"; since "5.7.10" }
    field { name "preparationReasons"; desc "当前不能准备交换盘的原因代码；为空数组表示可以准备。"; type "List"; since "5.7.10" }
    ref { name "candidates"; path "org.zstack.kvm.memory.MemorySwapDiskInventory.candidates"; desc "物理机上的块设备，及能否作为交换盘。"; type "List"; since "5.7.10"; clz MemorySwapDiskDeviceInventory.class }
}
