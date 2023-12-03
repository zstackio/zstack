package org.zstack.header.identity

import org.zstack.header.identity.APICreatePolicyEvent

doc {
    title "CreatePolicy"

    category "identity"

    desc """创建策略"""

    rest {
        request {
			url "POST /v1/accounts/policies"

			header (Authorization: 'OAuth the-session-uuid')

            clz APICreatePolicyMsg.class

            desc """创建策略"""
            
			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "statements"
					enclosedIn "params"
					desc "策略声明"
					location "body"
					type "List"
					optional false
					since "0.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
        }

        response {
            clz APICreatePolicyEvent.class
        }
    }
}