package org.zstack.kvm.memory

import java.sql.Timestamp

doc {
    title "物理机内存优化状态"
    field { name "hostUuid"; desc "物理机UUID。"; type "String"; since "5.5.48" }
    field { name "status"; desc "Applied（已生效）、Applying（已下发待返回）、Converging（同步中：等待条件或重发中）或Failed（连续失败或被拒绝，reason为原因码）。"; type "String"; since "5.5.48" }
    field { name "featureState"; desc "Active、Disabled、Paused、Unknown或Unsupported；观测过期时为Unknown。"; type "String"; since "5.5.48" }
    field { name "zramState"; desc "ZRAM本身是否在工作，与内存同页合并和配置下发状态无关：Active（正在回收）、Inactive（压缩池在但未回收）、Preparing（压缩池未建好）、Paused、Drained、Disabled（配置关闭）、Unsupported、Failed（下发失败）、Unknown（无观测或服务未运行）；非Active时原因见reason。"; type "String"; since "5.5.48" }
    field { name "reason"; desc "阻塞或失败原因。"; type "String"; since "5.5.48" }
    field { name "allowedActions"; desc "当前可执行的操作，前端据此显示按钮。"; type "List"; since "5.5.48" }
    field { name "activeJobUuid"; desc "正在该物理机上执行的内存优化长任务UUID；没有时为空。"; type "String"; since "5.5.48" }
    field { name "lastObservedAt"; desc "最近一次观测时间，Unix epoch毫秒。"; type "Long"; since "5.5.48" }
    field { name "createDate"; desc "创建时间"; type "Timestamp"; since "5.5.48" }
    field { name "lastOpDate"; desc "最后一次修改时间"; type "Timestamp"; since "5.5.48" }
}
