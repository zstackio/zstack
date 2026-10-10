package org.zstack.kvm.memory

import org.zstack.kvm.memory.APIQueryMemoryTaskReply

doc {
    title "GetMemoryTasks"

    category "memoryOptimization"

    desc """按任务UUID、Host和状态查询管理节点任务记录；任务成功状态不能替代Host状态读回。"""

    rest {
        request {
			url "GET /v1/memory-optimization/tasks"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIGetMemoryTasksMsg.class

            desc """分页返回任务inventory及执行原因；使用snapshotId维持同一查询快照。"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "query"
					type "String"
					optional true
					since "5.5.38"
				}
				column {
					name "hostUuid"
					enclosedIn ""
					desc "物理机UUID"
					location "query"
					type "String"
					optional true
					since "5.5.38"
				}
				column {
					name "status"
					enclosedIn ""
					desc "按任务状态过滤，例如Queued、Running、Succeeded、Failed或Unknown。"
					location "query"
					type "String"
					optional true
					since "5.5.38"
				}
				column {
					name "start"
					enclosedIn ""
					desc "结果偏移量，从0开始。"
					location "query"
					type "int"
					optional true
					since "5.5.38"
				}
				column {
					name "limit"
					enclosedIn ""
					desc "页大小；默认100，最大500。"
					location "query"
					type "Integer"
					optional true
					since "5.5.38"
				}
				column {
					name "snapshotId"
					enclosedIn ""
					desc "续页快照令牌；查询版本冲突时重新从首段读取。"
					location "query"
					type "String"
					optional true
					since "5.5.38"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "5.5.38"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "5.5.38"
				}
			}
        }

        response {
            clz APIQueryMemoryTaskReply.class
        }
    }
}