package org.zstack.kvm.memory

import org.zstack.kvm.memory.APIGetVmMemoryOptimizationsReply

doc {
    title "GetVmMemoryOptimizations"

    category "memoryOptimization"

    desc """批量查询显式VM集合的内存优化统计；响应map按每个请求VM UUID给出结果或不可用原因。"""

    rest {
        request {
			url "GET /v1/vm-instances/memory-optimizations"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIGetVmMemoryOptimizationsMsg.class

            desc """VM指标单位为字节。超出整体查询期限的未完成VM仍会以不可用结果返回，不会被遗漏。"""

			params {

				column {
					name "vmUuids"
					enclosedIn ""
					desc "必填且非空的VM UUID集合；调用者仍须具备相应资源读取权限。"
					location "query"
					type "List"
					optional false
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
            clz APIGetVmMemoryOptimizationsReply.class
        }
    }
}