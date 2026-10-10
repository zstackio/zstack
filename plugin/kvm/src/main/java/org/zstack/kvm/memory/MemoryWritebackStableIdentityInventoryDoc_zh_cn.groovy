package org.zstack.kvm.memory

doc {
    title "写回候选稳定设备身份"
    field { name "bootId"; desc "采集时的Host启动ID。"; type "String"; since "5.5.38" }
    field { name "device"; desc "规范化设备路径。"; type "String"; since "5.5.38" }
    field { name "byIdPath"; desc "设备稳定别名。"; type "String"; since "5.5.38" }
    field { name "majorMinor"; desc "设备主次设备号。"; type "String"; since "5.5.38" }
    field { name "serial"; desc "设备序列号。"; type "String"; since "5.5.38" }
    field { name "wwn"; desc "设备WWN；未知时为空字符串。"; type "String"; since "5.5.38" }
    field { name "capacityBytes"; desc "设备容量，单位字节；未提供时为空。"; type "Long"; since "5.5.38" }
    field { name "transport"; desc "设备传输类型。"; type "String"; since "5.5.38" }
    field { name "sysfsPath"; desc "设备sysfs路径。"; type "String"; since "5.5.38" }
    field { name "sysfsInode"; desc "sysfs身份token。"; type "String"; since "5.5.38" }
    field { name "rdev"; desc "设备节点的原始设备号；未提供时为空。"; type "Long"; since "5.5.38" }
    field { name "nodeInode"; desc "设备节点inode；未提供时为空。"; type "Long"; since "5.5.38" }
}
