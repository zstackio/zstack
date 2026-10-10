package org.zstack.kvm.memory

import org.zstack.kvm.memory.APIGetVmMemoryOptimizationReply

doc {
    title "GetVmMemoryOptimization"

    category "memoryOptimization"

    desc """查询单个VM当前内存优化统计及质量/不可用原因；使用VM读取权限和当前实例身份校验。"""

    rest {
        request {
			url "GET /v1/vm-instances/{vmUuid}/memory-optimization"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIGetVmMemoryOptimizationMsg.class

            desc """VM指标单位为字节，是源memcg记账值，不代表VM净物理节省或Host总量分摊。"""

			params {

				column {
					name "vmUuid"
					enclosedIn ""
					desc "虚拟机UUID。"
					location "url"
					type "String"
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
            clz APIGetVmMemoryOptimizationReply.class
        }
    }
}