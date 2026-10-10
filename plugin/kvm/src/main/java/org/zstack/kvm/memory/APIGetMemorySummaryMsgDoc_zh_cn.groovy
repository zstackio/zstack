package org.zstack.kvm.memory

import org.zstack.kvm.memory.APIGetMemorySummaryReply

doc {
    title "GetMemorySummary"

    category "memoryOptimization"

    desc """汇总可覆盖Host的KSM/ZRAM内存节省估算和样本覆盖质量；不用于VM归因或历史趋势。"""

    rest {
        request {
			url "GET /v1/memory-summary"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIGetMemorySummaryMsg.class

            desc """所有字节字段均为bytes。sampleTime/currentStatusSampleTime为Unix epoch毫秒；缺失数据保持未知，不伪造为0。"""

			params {

				column {
					name "hostUuids"
					enclosedIn ""
					desc "当前KVM物理机UUID列表；省略为全部，显式空列表为空选择，重复UUID去重。不存在或非Host资源由标准资源校验拒绝，非KVM物理机拒绝；与zoneUuid取交集，合法空交集返回无覆盖而非伪造零收益。"
					location "query"
					type "List"
					optional true
					since "5.5.38"
				}
				column {
					name "zoneUuid"
					enclosedIn ""
					desc "区域UUID"
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
            clz APIGetMemorySummaryReply.class
        }
    }
}
