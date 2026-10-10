package org.zstack.kvm.memory

import org.zstack.kvm.memory.APIQueryMemoryStateReply

doc {
    title "GetMemoryStates"

    category "memoryOptimization"

    desc """查询Host当前内存优化观测和控制状态；本接口返回当前快照，不提供历史趋势。"""

    rest {
        request {
			url "GET /v1/memory-optimization/states"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIGetMemoryStatesMsg.class

            desc """兼容旧QueryMemoryState参数的显式只读分页接口。sampleTime为Unix epoch毫秒；指标中的字节字段单位为bytes。"""

			params {

				column {
					name "hostUuids"
					enclosedIn ""
					desc "可选当前KVM Host UUID列表；不存在、错误资源类型或非KVM目标拒绝。重复值去重；省略或空列表查询全部当前KVM Host，不同于Summary的显式空集语义。"
					location "query"
					type "List"
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
					desc "页大小；默认100，最大500，受memoryOptimization查询预算配置约束。"
					location "query"
					type "Integer"
					optional true
					since "5.5.38"
				}
				column {
					name "snapshotId"
					enclosedIn ""
					desc "续页快照令牌；后续页必须复用，快照版本冲突时从start=0重新查询。"
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
            clz APIQueryMemoryStateReply.class
        }
    }
}
