package org.zstack.header.identity

import org.zstack.header.identity.APIRemoveUserFromGroupEvent

doc {
    title "RemoveUserFromGroup"

    category "identity"

    desc """从用户组中移除用户"""

    rest {
        request {
			url "DELETE /v1/accounts/groups/{groupUuid}/users/{userUuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIRemoveUserFromGroupMsg.class

            desc """从用户组中移除用户"""
            
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
            clz APIRemoveUserFromGroupEvent.class
        }
    }
}