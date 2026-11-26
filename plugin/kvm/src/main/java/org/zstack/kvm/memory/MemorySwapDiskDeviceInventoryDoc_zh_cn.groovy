package org.zstack.kvm.memory

doc {
    title "交换盘或候选盘"
    field { name "id"; desc "候选盘ID，用于PrepareHostMemorySwapDisk；交换盘本身没有此字段。"; type "String"; since "5.7.10" }
    field { name "device"; desc "设备路径。"; type "String"; since "5.7.10" }
    field { name "byIdPath"; desc "带序列号的/dev/disk/by-id路径。"; type "String"; since "5.7.10" }
    field { name "serial"; desc "序列号。"; type "String"; since "5.7.10" }
    field { name "wwn"; desc "WWN。"; type "String"; since "5.7.10" }
    field { name "size"; desc "容量，字节。"; type "Long"; since "5.7.10" }
    field { name "state"; desc "Available：可作为交换盘；NeedsOverwriteConfirmation：盘上有数据，确认覆盖后可用；Ineligible：不可用，见reasons；Active：正在作为交换盘使用；Fault：交换盘有问题，见reasons。"; type "String"; since "5.7.10" }
    field { name "reasons"; desc "原因代码，例如NOT_LOCAL_NVME_DEVICE、SYSTEM_OR_MOUNTED_DEVICE、ZRAM_WRITEBACK_BACKEND、SWAP_DISK_NOT_FOUND。"; type "List"; since "5.7.10" }
    field { name "swapUuid"; desc "交换签名UUID，仅交换盘有。"; type "String"; since "5.7.10" }
    field { name "usedSize"; desc "已换出到交换盘的数据量，字节，仅启用中的交换盘有。"; type "Long"; since "5.7.10" }
}
