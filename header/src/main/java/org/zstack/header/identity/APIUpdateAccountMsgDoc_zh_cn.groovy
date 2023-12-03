package org.zstack.header.identity

import org.zstack.header.identity.APIUpdateAccountEvent

doc {
    title "UpdateAccount"

    category "identity"

    desc """更新账户"""

    rest {
        request {
			url "PUT /v1/accounts/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIUpdateAccountMsg.class

            desc """更新账户"""
            
			params {

				column {
					name "uuid"
					enclosedIn "updateAccount"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "password"
					enclosedIn "updateAccount"
					desc "密码"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "name"
					enclosedIn "updateAccount"
					desc "账户名称"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "updateAccount"
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
				column {
					name "oldPassword"
					enclosedIn "updateAccount"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.6.0"
				}
			}
        }

        response {
            clz APIUpdateAccountEvent.class
        }
    }
}