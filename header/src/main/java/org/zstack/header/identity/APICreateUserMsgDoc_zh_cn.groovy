package org.zstack.header.identity

import org.zstack.header.identity.APICreateUserEvent

doc {
    title "CreateUser"

    category "identity"

    desc """创建用户"""

    rest {
        request {
			url "POST /v1/accounts/users"

			header (Authorization: 'OAuth the-session-uuid')

            clz APICreateUserMsg.class

            desc """创建用户"""
            
			params {

				column {
					name "name"
					enclosedIn "params"
					desc "用户名称"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "password"
					enclosedIn "params"
					desc "密码"
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
            clz APICreateUserEvent.class
        }
    }
}