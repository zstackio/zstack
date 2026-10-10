package org.zstack.kvm.memory

doc {
    title "维护回执中的目标设备身份"
    field { name "bootId"; desc "Host启动ID。"; type "String"; since "5.5.38" }
    field { name "canonicalDevice"; desc "规范化设备路径。"; type "String"; since "5.5.38" }
    field { name "rdev"; desc "设备节点原始设备号；未提供时为空。"; type "Long"; since "5.5.38" }
    field { name "nodeInode"; desc "设备节点inode；未提供时为空。"; type "Long"; since "5.5.38" }
    field { name "sysfsInode"; desc "sysfs inode；未提供时为空。"; type "Long"; since "5.5.38" }
    field { name "capacityBytes"; desc "设备容量，单位字节；未提供时为空。"; type "Long"; since "5.5.38" }
    field { name "filePath"; desc "受管后端文件路径；块设备后端省略。"; type "String"; since "5.5.38" }
    field { name "fileInode"; desc "后端文件inode；块设备后端省略，未提供时为空。"; type "Long"; since "5.5.38" }
    field { name "fileDevice"; desc "后端文件所在文件系统设备号；块设备后端省略，未提供时为空。"; type "Long"; since "5.5.38" }
    field { name "fileSize"; desc "后端文件大小，单位字节；块设备后端省略，未提供时为空。"; type "Long"; since "5.5.38" }
    field { name "directIo"; desc "是否使用direct IO；未提供时为空。"; type "Boolean"; since "5.5.38" }
    field { name "stackSha256"; desc "底层设备栈摘要。"; type "String"; since "5.5.38" }
    field { name "fileStackSha256"; desc "后端文件设备栈摘要；块设备后端省略。"; type "String"; since "5.5.38" }
}
