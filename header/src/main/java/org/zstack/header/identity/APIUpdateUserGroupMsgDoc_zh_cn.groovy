package org.zstack.header.identity

import org.zstack.header.identity.APIUpdateUserGroupEvent

doc {
    title "UpdateUserGroup"

    category "identity"

    desc """更新用户组"""

    rest {
        request {
			url "PUT /v1/accounts/groups/actions"

			header (Authorization: 'OAuth the-session-uuid')


            clz APIUpdateUserGroupMsg.class

            desc """更新用户组"""
            
			params {

				column {
					name "uuid"
					enclosedIn "updateUserGroup"
					desc "资源的UUID，唯一标示该资源"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "name"
					enclosedIn "updateUserGroup"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "description"
					enclosedIn "updateUserGroup"
					desc "资源的详细描述"
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
			}
        }

        response {
            clz APIUpdateUserGroupEvent.class
        }
    }
}