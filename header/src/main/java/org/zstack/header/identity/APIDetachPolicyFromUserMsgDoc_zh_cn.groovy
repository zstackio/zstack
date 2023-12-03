package org.zstack.header.identity

import org.zstack.header.identity.APIDetachPolicyFromUserEvent

doc {
    title "DetachPolicyFromUser"

    category "identity"

    desc """将策略从用户解绑"""

    rest {
        request {
			url "DELETE /v1/accounts/users/{userUuid}/policies/{policyUuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIDetachPolicyFromUserMsg.class

            desc """将策略从用户解绑"""
            
			params {

				column {
					name "policyUuid"
					enclosedIn ""
					desc "权限策略UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
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
            clz APIDetachPolicyFromUserEvent.class
        }
    }
}