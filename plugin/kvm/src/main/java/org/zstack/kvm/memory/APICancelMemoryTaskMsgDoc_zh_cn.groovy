package org.zstack.kvm.memory

import org.zstack.kvm.memory.APICancelMemoryTaskEvent

doc {
    title "CancelMemoryTask"

    category "memoryOptimization"

    desc """取消尚未派发的排队任务；不会删除历史记录，也不能撤销已开始的Host操作。"""

    rest {
        request {
			url "PUT /v1/memory-optimization/tasks/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APICancelMemoryTaskMsg.class

            desc """请求体使用 `cancelMemoryTask` action wrapper。旧 PUT /v1/memory-tasks/{uuid}/actions 保留为同一API的兼容别名；新调用使用规范路径。仅取消尚未派发的任务，运行中或Unknown任务不可取消。"""

			params {

				column {
					name "uuid"
					enclosedIn "cancelMemoryTask"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.5.38"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.5.38"
				}
			}
        }

        response {
            clz APICancelMemoryTaskEvent.class
        }
    }
}
