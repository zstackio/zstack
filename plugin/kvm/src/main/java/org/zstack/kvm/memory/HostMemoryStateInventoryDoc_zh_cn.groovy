package org.zstack.kvm.memory

import java.sql.Timestamp

doc {
    title "物理机内存优化状态"
    field { name "hostUuid"; desc "物理机UUID。"; type "String"; since "5.5.48" }
    field { name "status"; desc "Applied、Applying、Blocked、Failed或Unknown。"; type "String"; since "5.5.48" }
    field { name "featureState"; desc "Active、Disabled、Paused、Unknown或Unsupported；观测过期时为Unknown。"; type "String"; since "5.5.48" }
    field { name "reason"; desc "阻塞或失败原因。"; type "String"; since "5.5.48" }
    field { name "capabilities"; desc "ksm、ksmZeroPages、zram、writeback是否支持及不支持的原因。"; type "Map"; since "5.5.48" }
    field { name "allowedActions"; desc "当前可执行的操作，前端据此显示按钮；含recoverUncertain时同时返回uncertainRecovery。"; type "List"; since "5.5.48" }
    field { name "uncertainRecovery"; desc "RecoverHostMemoryOptimization的确认参数，原样回传；不可确认恢复时为空。"; type "Map"; since "5.5.48" }
    field { name "activeJobUuid"; desc "正在该物理机上执行的内存优化长任务UUID；没有时为空。"; type "String"; since "5.5.48" }
    field { name "lastObservedAt"; desc "最近一次观测时间，Unix epoch毫秒。"; type "Long"; since "5.5.48" }
    field { name "createDate"; desc "创建时间"; type "Timestamp"; since "5.5.48" }
    field { name "lastOpDate"; desc "最后一次修改时间"; type "Timestamp"; since "5.5.48" }
}
