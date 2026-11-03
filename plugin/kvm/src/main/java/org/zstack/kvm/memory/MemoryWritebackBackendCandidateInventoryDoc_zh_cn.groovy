package org.zstack.kvm.memory

doc {
    title "写回盘候选"
    field { name "id"; desc "盘的标识，准备写回盘时传入；物理机重启或换盘后会变化。"; type "String"; since "5.5.48" }
    field { name "resourceUuid"; desc "已登记为写回盘时的资源UUID；未登记时为空。"; type "String"; since "5.5.48" }
    field { name "type"; desc "block或file。"; type "String"; since "5.5.48" }
    field { name "path"; desc "显示用路径，优先使用by-id路径。"; type "String"; since "5.5.48" }
    field { name "device"; desc "设备名，如/dev/vdb。"; type "String"; since "5.5.48" }
    field { name "size"; desc "整盘容量，单位字节。"; type "Long"; since "5.5.48" }
    field { name "serial"; desc "磁盘序列号；未知时为空。"; type "String"; since "5.5.48" }
    field { name "wwn"; desc "磁盘WWN；未知时为空。"; type "String"; since "5.5.48" }
    field { name "transport"; desc "传输类型，如virtio、sata、nvme；未知时为空。"; type "String"; since "5.5.48" }
    field { name "state"; desc "Available：空盘，可以准备；Prepared：已准备，未在使用；Active：正在作为写回盘使用；Ineligible：不符合条件；Disconnected：之前准备过的盘现在找不到；IdentityMismatch：路径还在，但已不是当初准备的那块盘。"; type "String"; since "5.5.48" }
    field { name "reasons"; desc "不能准备或异常的原因代码。"; type "List"; since "5.5.48" }
}
