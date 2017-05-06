package org.zstack.header.identity

import org.zstack.header.identity.APIUpdateUserEvent

doc {
    title "UpdateUser"

    category "identity"

    desc """更新用户"""

    rest {
        request {
			url "PUT /v1/accounts/users/actions"

			header (Authorization: 'OAuth the-session-uuid')


            clz APIUpdateUserMsg.class

            desc """更新用户"""
            
			params {

				column {
					name "uuid"
					enclosedIn "updateUser"
					desc "资源的UUID，唯一标示该资源"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "password"
					enclosedIn "updateUser"
					desc "密码"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "name"
					enclosedIn "updateUser"
					desc "用户名称"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "description"
					enclosedIn "updateUser"
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
            clz APIUpdateUserEvent.class
        }
    }
}