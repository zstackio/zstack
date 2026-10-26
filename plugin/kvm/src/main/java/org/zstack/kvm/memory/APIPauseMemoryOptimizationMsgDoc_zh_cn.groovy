package org.zstack.kvm.memory

import org.zstack.kvm.memory.APIPauseMemoryOptimizationEvent

doc {
    title "PauseMemoryOptimization"

    category "memoryOptimization"

    desc """暂停集群内物理机的内存优化（以长任务提交）。后端在集群内逐台下发并等待每台完成，同一台物理机上的操作按提交顺序执行；长任务结果为物理机UUID到结果的映射（success，或最终状态与原因）。"""

    rest {
        request {
			url "PUT /v1/clusters/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIPauseMemoryOptimizationMsg.class

            desc """暂停集群内物理机的内存优化（以长任务提交）"""

			params {

				column {
					name "uuid"
					enclosedIn "pauseMemoryOptimization"
					desc "集群UUID"
					location "url"
					type "String"
					optional false
					since "5.5.48"
				}
				column {
					name "hostUuid"
					enclosedIn "pauseMemoryOptimization"
					desc "只处理集群内的这一台物理机；不填则逐台处理集群内全部KVM物理机"
					location "body"
					type "String"
					optional true
					since "5.5.48"
				}
				column {
					name "resourceUuid"
					enclosedIn "pauseMemoryOptimization"
					desc "长任务的UUID，可选"
					location "body"
					type "String"
					optional true
					since "5.5.48"
				}
				column {
					name "tagUuids"
					enclosedIn "pauseMemoryOptimization"
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
            clz APIPauseMemoryOptimizationEvent.class
        }
    }
}
