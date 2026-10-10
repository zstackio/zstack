package org.zstack.kvm.memory

import org.zstack.kvm.memory.APIQueryHostMemoryOperationsReply

doc {
    title "GetHostMemoryOperations"

    category "memoryOptimization"

    desc """读取指定Host本地操作日志；与管理节点MemoryTask任务表不同，operationId标识Agent/Go本地操作。"""

    rest {
        request {
			url "GET /v1/hosts/{hostUuid}/memory-optimization/operations"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIGetHostMemoryOperationsMsg.class

            desc """Host限定的只读诊断查询。start为偏移量，limit默认100且最大500。"""

			params {

				column {
					name "hostUuid"
					enclosedIn ""
					desc "物理机UUID"
					location "url"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "operationId"
					enclosedIn ""
					desc "可选操作ID过滤条件。"
					location "query"
					type "String"
					optional true
					since "5.5.38"
				}
				column {
					name "vmUuid"
					enclosedIn ""
					desc "可选VM UUID过滤条件。"
					location "query"
					type "String"
					optional true
					since "5.5.38"
				}
				column {
					name "status"
					enclosedIn ""
					desc "可选操作状态过滤条件。"
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
            clz APIQueryHostMemoryOperationsReply.class
        }
    }
}