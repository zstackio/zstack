package org.zstack.kvm.memory

import org.zstack.kvm.memory.APIRecoverHostMemoryOptimizationEvent

doc {
    title "RecoverHostMemoryOptimization"

    category "memoryOptimization"

    desc """确认恢复结果不明的物理机（以长任务提交）：排空后“恢复优化”的激活结果不明时作废这次激活、物理机保持排空；一次配置下发结果不明时核实已生效部分并收尾。仅当物理机状态的allowedActions含recoverUncertain时可用，参数为状态中uncertainRecovery的值，原样回传。核实失败或状态已变化时长任务失败并给出原因。"""

    rest {
        request {
			url "PUT /v1/hosts/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIRecoverHostMemoryOptimizationMsg.class

            desc """确认恢复结果不明的物理机"""

			params {

				column {
					name "uuid"
					enclosedIn "recoverHostMemoryOptimization"
					desc "物理机UUID"
					location "url"
					type "String"
					optional false
					since "5.5.48"
				}
				column {
					name "expectedControlOperationUuid"
					enclosedIn "recoverHostMemoryOptimization"
					desc "uncertainRecovery.expectedControlOperationUuid，即结果不明的那次操作"
					location "body"
					type "String"
					optional false
					since "5.5.48"
				}
				column {
					name "expectedHostBootId"
					enclosedIn "recoverHostMemoryOptimization"
					desc "uncertainRecovery.expectedHostBootId"
					location "body"
					type "String"
					optional false
					since "5.5.48"
				}
				column {
					name "expectedPoolGeneration"
					enclosedIn "recoverHostMemoryOptimization"
					desc "uncertainRecovery.expectedPoolGeneration"
					location "body"
					type "String"
					optional false
					since "5.5.48"
				}
				column {
					name "drainControlOperationUuid"
					enclosedIn "recoverHostMemoryOptimization"
					desc "uncertainRecovery.drainControlOperationUuid；排空后的激活才有，没有就不传"
					location "body"
					type "String"
					optional true
					since "5.5.48"
				}
				column {
					name "resourceUuid"
					enclosedIn "recoverHostMemoryOptimization"
					desc "长任务的UUID，可选"
					location "body"
					type "String"
					optional true
					since "5.5.48"
				}
				column {
					name "tagUuids"
					enclosedIn "recoverHostMemoryOptimization"
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
            clz APIRecoverHostMemoryOptimizationEvent.class
        }
    }
}
