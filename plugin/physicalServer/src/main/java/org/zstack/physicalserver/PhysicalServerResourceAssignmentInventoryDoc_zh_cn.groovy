package org.zstack.physicalserver

doc {
    title "物理服务器资源分配清单"

    field {
        name "uuid"
        desc "资源分配UUID"
        type "String"
        since "5.5.38"
    }
    field {
        name "serverUuid"
        desc "物理服务器UUID"
        type "String"
        since "5.5.38"
    }
    field {
        name "roleType"
        desc "可控制的资源使用角色；第一期为MANAGEMENT或COMPUTE"
        type "String"
        since "5.5.38"
    }
    field {
        name "cpuSet"
        desc "分配给该Role的CPU集合，例如0-3,8-11"
        type "String"
        since "5.5.38"
    }
    field {
        name "memory"
        desc "应用到该Role Slice的总体内存上限，单位字节；Slice内服务共享该边界，0表示不限，空表示不控制内存"
        type "Long"
        since "5.5.38"
    }
    field {
        name "state"
        desc "资源分配状态：Unsynced表示尚未完成同步，Synced表示Role Slice边界及必选服务归属已校验一致"
        type "String"
        since "5.5.38"
    }
    field {
        name "createDate"
        desc "创建时间"
        type "Timestamp"
        since "5.5.38"
    }
    field {
        name "lastOpDate"
        desc "最后一次修改时间"
        type "Timestamp"
        since "5.5.38"
    }
}
