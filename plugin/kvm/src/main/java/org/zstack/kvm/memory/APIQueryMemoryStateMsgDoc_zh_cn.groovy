package org.zstack.kvm.memory

import org.zstack.kvm.memory.APIQueryMemoryStateReply

doc {
    title "QueryMemoryState"

    category "memoryOptimization"

    desc """旧路由兼容入口；新客户端请使用 `/memory-optimization/states`。响应和分页语义与GetMemoryStates一致。"""

    rest {
        request {
			url "GET /v1/memory-states"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIQueryMemoryStateMsg.class

            desc """GET无请求体。sampleTime和状态采样时间为Unix epoch毫秒，指标内存量为字节。"""

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
					desc "页大小；默认100，最大500。"
					location "query"
					type "Integer"
					optional true
					since "5.5.38"
				}
				column {
					name "snapshotId"
					enclosedIn ""
					desc "续页快照令牌；版本冲突时清除令牌并从0重新开始。"
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
