package org.zstack.header.identity

import org.zstack.header.identity.APIAddUserToGroupEvent

doc {
    title "AddUserToGroup"

    category "identity"

    desc """将用于添加到用户组"""

    rest {
        request {
			url "POST /v1/accounts/groups/{groupUuid}/users"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIAddUserToGroupMsg.class

            desc """将用于添加到用户组"""
            
			params {

				column {
					name "userUuid"
					enclosedIn "params"
					desc "用户UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "groupUuid"
					enclosedIn "params"
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
            clz APIAddUserToGroupEvent.class
        }
    }
}