package org.zstack.kvm.memory

import org.zstack.kvm.memory.APIDeleteMemoryTaskEvent
import org.zstack.kvm.memory.APIDeleteMemoryTaskMsg

doc {
    title "DeleteMemoryTask"
    category "内存优化"
    desc "显式清理一个已结束且无运行/恢复引用的内存任务根任务及其子任务。该操作只删除管理面任务历史，不改变物理机策略或运行状态。重复删除或目标不存在时返回 deleted=false；这不表示此前操作失败。墓碑仍会阻止相同 clientRequestUuid 的旧请求重新执行。正在执行、结果未知、仍被状态/控制/迁移/恢复任务引用、包含 Blocked 子任务或保留目标快照提交回放信息的任务不可清理。"

    rest {
        request {
            desc "仅管理员可调用。uuid 必须是根任务 UUID；不能单独删除 Host 子任务。"
            url "DELETE /v1/memory-optimization/tasks/{uuid}"
            header (Authorization: 'OAuth the-session-uuid')
            clz APIDeleteMemoryTaskMsg.class
            params {
                column {
                    name "uuid"
                    enclosedIn ""
                    desc "任务根UUID；子任务UUID会被拒绝。"
                    location "url"
                    type "String"
                    optional false
                    since "5.5.38"
                }
            }
        }
        response {
            clz APIDeleteMemoryTaskEvent.class
        }
    }
}
