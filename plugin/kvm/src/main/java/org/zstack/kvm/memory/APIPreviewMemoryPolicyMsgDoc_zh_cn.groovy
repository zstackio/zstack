package org.zstack.kvm.memory

import org.zstack.kvm.memory.APIPreviewMemoryPolicyReply

doc {
    title "PreviewMemoryPolicy"

    category "memoryOptimization"

    desc """只读预览策略变化及受影响Host的资格和阻塞字段；不会保存配置或派发任务。"""

    rest {
        request {
			url "POST /v1/memory-policies/{resourceUuid}/preview"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIPreviewMemoryPolicyMsg.class

            desc """请求体使用params wrapper；policy为JSON字符串。新调用省略scope，按URL中的resourceUuid推导资源类型（全局使用global）；旧scope仅兼容且冲突时拒绝。"""

			params {

				column {
					name "scope"
					enclosedIn "params"
					desc "仅旧客户端兼容字段；新客户端省略。传入时必须与资源UUID身份一致。"
					location "body"
					type "String"
					optional true
					since "5.5.38"
					values ("Global","Cluster","Host","VM")
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "url"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "policy"
					enclosedIn "params"
					desc "要预览的策略JSON字符串。"
					location "body"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "targetHostUuids"
					enclosedIn "params"
					desc "可选目标Host UUID列表；用于显式范围预览。"
					location "body"
					type "List"
					optional true
					since "5.5.38"
				}
				column {
					name "action"
					enclosedIn "params"
					desc "预览动作，通常为apply；只计算结果，不产生副作用。"
					location "body"
					type "String"
					optional true
					since "5.5.38"
					values ("apply","clearOverride")
				}
				column {
					name "clearOverrideFields"
					enclosedIn "params"
					desc "模拟清除的字段覆盖名列表。"
					location "body"
					type "List"
					optional true
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
            clz APIPreviewMemoryPolicyReply.class
        }
    }
}
