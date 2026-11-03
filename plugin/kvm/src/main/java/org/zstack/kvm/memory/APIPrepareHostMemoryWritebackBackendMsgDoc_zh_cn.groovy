package org.zstack.kvm.memory

import org.zstack.kvm.memory.APIPrepareHostMemoryWritebackBackendEvent

doc {
    title "PrepareHostMemoryWritebackBackend"

    category "memoryOptimization"

    desc """清空并绑定物理机上的一块空盘作为ZRAM写回盘（以长任务提交）。candidateId取自GetHostMemoryWritebackBackends；管理节点在清空前向物理机核对该盘仍存在且可以准备，否则长任务以MEMORY_BACKEND_PREPARATION_INVALID失败并给出原因。会清空该盘，调用前应由界面二次确认。与该物理机上的其他内存优化操作按提交顺序执行。"""

    rest {
        request {
			url "PUT /v1/hosts/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIPrepareHostMemoryWritebackBackendMsg.class

            desc """清空并绑定物理机上的一块空盘作为ZRAM写回后端"""
            
			params {

				column {
					name "uuid"
					enclosedIn "prepareHostMemoryWritebackBackend"
					desc "物理机UUID"
					location "url"
					type "String"
					optional false
					since "5.5.48"
				}
				column {
					name "candidateId"
					enclosedIn "prepareHostMemoryWritebackBackend"
					desc "GetHostMemoryWritebackBackends返回的candidates[].id"
					location "body"
					type "String"
					optional false
					since "5.5.48"
				}
				column {
					name "resourceUuid"
					enclosedIn "prepareHostMemoryWritebackBackend"
					desc "长任务的UUID，可选"
					location "body"
					type "String"
					optional true
					since "5.5.48"
				}
				column {
					name "tagUuids"
					enclosedIn "prepareHostMemoryWritebackBackend"
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
            clz APIPrepareHostMemoryWritebackBackendEvent.class
        }
    }
}