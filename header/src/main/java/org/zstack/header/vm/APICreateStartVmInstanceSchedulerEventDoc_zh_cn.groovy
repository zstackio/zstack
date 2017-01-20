package org.zstack.header.vm

import org.zstack.header.core.scheduler.SchedulerInventory
import org.zstack.header.errorcode.ErrorCode

doc {

    title "创建启动云主机的定时任务"

    ref {
        name "error"
        path "org.zstack.header.vm.APICreateStartVmInstanceSchedulerEvent.error"
        desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
        type "ErrorCode"
        since "1.6"
        clz ErrorCode.class
    }
    ref {
        name "inventory"
        path "org.zstack.header.vm.APICreateStartVmInstanceSchedulerEvent.inventory"
        desc "null"
        type "SchedulerInventory"
        since "1.6"
        clz SchedulerInventory.class
    }
}
