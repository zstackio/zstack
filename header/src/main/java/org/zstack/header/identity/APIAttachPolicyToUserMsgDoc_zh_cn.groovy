package org.zstack.header.identity

import org.zstack.header.identity.APIAttachPolicyToUserEvent

doc {
    title "AttachPolicyToUser"

    category "identity"

    desc """绑定策略到用户"""

    rest {
        request {
			url "POST /v1/accounts/users/{userUuid}/policies"

			header (Authorization: 'OAuth the-session-uuid')


            clz APIAttachPolicyToUserMsg.class

            desc """绑定策略到用户"""
            
			params {

				column {
					name "userUuid"
					enclosedIn ""
					desc "用户UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "policyUuid"
					enclosedIn ""
					desc "权限策略UUID"
					location "body"
					type "String"
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
            clz APIAttachPolicyToUserEvent.class
        }
    }
}