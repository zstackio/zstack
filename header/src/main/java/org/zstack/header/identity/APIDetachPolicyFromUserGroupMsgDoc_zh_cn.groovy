package org.zstack.header.identity

import org.zstack.header.identity.APIDetachPolicyFromUserGroupEvent

doc {
    title "DetachPolicyFromUserGroup"

    category "identity"

    desc """将策略从用户组解绑"""

    rest {
        request {
			url "DELETE /v1/accounts/groups/{groupUuid}/policies/{policyUuid}"

			header (Authorization: 'OAuth the-session-uuid')


            clz APIDetachPolicyFromUserGroupMsg.class

            desc """将策略从用户组解绑"""
            
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
					name "groupUuid"
					enclosedIn ""
					desc "用户组UUID"
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
            clz APIDetachPolicyFromUserGroupEvent.class
        }
    }
}