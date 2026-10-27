package org.zstack.kvm.memory

import org.zstack.kvm.memory.APIRecoverVmMemoryMigrationHoldEvent

doc {
    title "RecoverVmMemoryMigrationHold"

    category "memoryOptimization"

    desc """解除云主机的内存迁移锁（以长任务提交，仅供运维使用）。热迁移结果无法自动核实时，迁移锁会一直保留在源物理机上，期间源物理机暂停主动回收与写回，其上开启ZRAM优化的云主机都无法热迁移，自动核对会在每次观测源物理机后重试。运维先在源、目标物理机上确认云主机实际所在（两边都有时先停掉不该存在的实例），再调用本接口：confirmedHostUuid传云主机当前所在的物理机（必须是这次迁移的源或目标），云主机已停止时不传。平台和两台物理机都核实一致后，在所在物理机上恢复绑定、撤销另一台的绑定并释放源物理机上的锁；核实不一致时长任务失败并给出原因，不做任何修改。"""

    rest {
        request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIRecoverVmMemoryMigrationHoldMsg.class

            desc """解除云主机的内存迁移锁"""

			params {

				column {
					name "uuid"
					enclosedIn "recoverVmMemoryMigrationHold"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "5.5.48"
				}
				column {
					name "confirmedHostUuid"
					enclosedIn "recoverVmMemoryMigrationHold"
					desc "运维确认的云主机当前所在物理机，必须是这次迁移的源或目标；云主机已停止时不传"
					location "body"
					type "String"
					optional true
					since "5.5.48"
				}
				column {
					name "resourceUuid"
					enclosedIn "recoverVmMemoryMigrationHold"
					desc "长任务的UUID，可选"
					location "body"
					type "String"
					optional true
					since "5.5.48"
				}
				column {
					name "tagUuids"
					enclosedIn "recoverVmMemoryMigrationHold"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "5.5.48"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.5.48"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.5.48"
				}
			}
        }

        response {
            clz APIRecoverVmMemoryMigrationHoldEvent.class
        }
    }
}
