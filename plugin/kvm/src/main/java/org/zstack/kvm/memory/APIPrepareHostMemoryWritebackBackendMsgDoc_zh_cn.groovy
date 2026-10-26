package org.zstack.kvm.memory

import org.zstack.kvm.memory.APIPrepareHostMemoryWritebackBackendEvent

doc {
    title "PrepareHostMemoryWritebackBackend"

    category "memoryOptimization"

    desc """清空并绑定物理机上的一块空盘作为ZRAM写回后端（以长任务提交）。参数取自GetHostMemoryWritebackBackends，用于确认要清空的正是该设备；后端自动填写版本与控制标识，与该物理机上的其他内存优化操作按提交顺序执行。准备失败时长任务失败并给出原因。"""

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
					desc "GetHostMemoryWritebackBackends返回的候选设备identity"
					location "body"
					type "String"
					optional false
					since "5.5.48"
				}
				column {
					name "expectedHostBootId"
					enclosedIn "prepareHostMemoryWritebackBackend"
					desc "GetHostMemoryWritebackBackends返回的bootId，物理机重启后需重新查询"
					location "body"
					type "String"
					optional false
					since "5.5.48"
				}
				column {
					name "expectedPoolGeneration"
					enclosedIn "prepareHostMemoryWritebackBackend"
					desc "GetHostMemoryWritebackBackends返回的poolGeneration（首次使用为none）"
					location "body"
					type "String"
					optional false
					since "5.5.48"
				}
				column {
					name "backendCapacityBytes"
					enclosedIn "prepareHostMemoryWritebackBackend"
					desc "候选设备的整盘容量（capacityBytes），单位字节"
					location "body"
					type "long"
					optional false
					since "5.5.48"
				}
				column {
					name "resetConfirmed"
					enclosedIn "prepareHostMemoryWritebackBackend"
					desc "确认清空该设备，必须为true"
					location "body"
					type "boolean"
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
