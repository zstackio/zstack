package org.zstack.kvm.memory

import org.zstack.header.errorcode.ErrorCode

doc {
    title "DeleteMemoryTaskEvent"
    category "内存优化"

    field {
        name "success"
        desc "API请求是否成功处理。"
        type "boolean"
        since "5.5.38"
    }
    ref {
        name "error"
        path "org.zstack.kvm.memory.APIDeleteMemoryTaskEvent.error"
        desc "删除被拒绝时的错误码。", false
        type "ErrorCode"
        since "5.5.38"
        clz ErrorCode.class
    }
    field {
        name "taskUuid"
        desc "请求清理的根任务UUID。"
        type "String"
        since "5.5.38"
    }
    field {
        name "deleted"
        desc "本次是否实际删除了任务记录；目标不存在或已清理时为false。"
        type "boolean"
        since "5.5.38"
    }
    field {
        name "scope"
        desc "已删除任务的策略作用域，仅在实际删除时返回。"
        type "String"
        since "5.5.38"
    }
    field {
        name "resourceUuid"
        desc "已删除任务的作用域资源UUID，仅在实际删除时返回。"
        type "String"
        since "5.5.38"
    }
    field {
        name "hostUuids"
        desc "锁定任务树时验证得到的物理机UUID集合，用于审计关联。"
        type "List"
        since "5.5.38"
    }
}
