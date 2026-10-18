package org.zstack.kvm.memory

import org.zstack.kvm.memory.APIQueryMemoryStateReply

doc {
    title "GetMemoryStates"

    category "memoryOptimization"

    desc """查询Host当前内存优化观测和控制状态；本接口返回当前快照，不提供历史趋势，也不触发新的Agent采样、恢复或重放操作。普通配置先走标准GlobalConfig/ResourceConfig保存，再读取本接口展示结果，不以前置status作为合法保存的硬准入。后端仅在原完成回执和独立当前证明均通过时自动收尾并接续最新配置，否则仍保留Unknown及旧操作保护。选盘BFF为形成单次prepare请求仍需读取同Host的管理端controlOperationUuid，不得取候选上的Agent控制标识代替。前端仅映射inventory.status的七种状态，详见MemoryStateInventory；currentStep、policyPlanStatus、reason和state供诊断，不作为前端组合判断条件。inventory.reason表示控制/任务结果；inventory.state是JSON字符串，包含最近一次被接受的Agent状态快照。客户端可读取JSON.parse(inventories[0].state).host_zram.reason；JSON.parse(inventories[0].state).backend_health.reason属于同一当前运行诊断域。NO_QUALIFIED_MANAGED_POOL: EXCLUSIVE_ZRAM_ROUTE_REQUIRED是原因值片段示例，不是完整States回包。结合嵌套对象自己的原生observed_at阅读，不以指标quality或lastSampleTime替代。Agent可保留池初始化拒绝原因直至受支持的恢复重新初始化；即使snapshot的observed_at更新，原因仍可能描述上一次初始化拒绝，不能据此断言磁盘swap此刻仍启用，需结合受支持的Host诊断。平台不会自动swapoff。新观测不会把当前诊断改写成原任务结果、请求版本或原计划；若现有观察路径接受可信控制回执，实际applied revision仍可更新。若非目标系统swap阻断建池，管理员应按既有Host维护及服务维护/核实路径处理；不可仅凭较晚观测重放或重试未决任务。配置已应用不等于持续压缩或已有内存节省。"""

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
