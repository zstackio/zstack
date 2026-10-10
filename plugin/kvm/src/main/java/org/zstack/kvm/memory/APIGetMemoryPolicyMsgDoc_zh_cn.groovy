package org.zstack.kvm.memory

import org.zstack.kvm.memory.APIGetMemoryPolicyReply

doc {
    title "GetMemoryPolicy"

    category "memoryOptimization"

    desc """读取指定作用域的内存优化策略、当前生效配置、revision、来源和可用动作。"""

    rest {
        request {
			url "GET /v1/memory-policies/{resourceUuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIGetMemoryPolicyMsg.class

            desc """新调用只提供URL中的resourceUuid：global表示全局，其余按平台ResourceVO推导Host、Cluster或VM。scope仅兼容旧调用；若仍传入必须与真实资源匹配。不存在、错误类型及非KVM的Host/Cluster拒绝，不回退到全局。"""

			params {

				column {
					name "scope"
					enclosedIn ""
					desc "仅旧客户端兼容字段；新客户端省略。传入时必须与资源UUID身份一致。"
					location "query"
					type "String"
					optional true
					since "5.5.38"
					values ("Global","Cluster","Host","VM")
				}
				column {
					name "resourceUuid"
					enclosedIn ""
					desc "资源UUID"
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
            clz APIGetMemoryPolicyReply.class
        }
    }
}
