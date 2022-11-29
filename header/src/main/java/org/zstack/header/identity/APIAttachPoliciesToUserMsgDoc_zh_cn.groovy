package org.zstack.header.identity

import org.zstack.header.identity.APIAttachPoliciesToUserEvent

doc {
    title "AttachPoliciesToUser"

    category "identity"

    desc """将策略绑定到用户"""

    rest {
        request {
			url "POST /v1/accounts/users/{userUuid}/policy-collection"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIAttachPoliciesToUserMsg.class

            desc """将策略绑定到用户"""
            
			params {

				column {
					name "userUuid"
					enclosedIn "params"
					desc "用户UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "policyUuids"
					enclosedIn "params"
					desc "策略的UUID列表"
					location "body"
					type "List"
					optional false
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
			}
        }

        response {
            clz APIAttachPoliciesToUserEvent.class
        }
    }
}