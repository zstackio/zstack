package org.zstack.header.identity

import org.zstack.header.identity.APICreateAccountEvent

doc {
    title "CreateAccount"

    category "identity"

    desc """创建账户"""

    rest {
        request {
			url "POST /v1/accounts"

			header (Authorization: 'OAuth the-session-uuid')


            clz APICreateAccountMsg.class

            desc """创建账户"""
            
			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
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
					name "type"
					enclosedIn "params"
					desc "账户类型"
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("SystemAdmin","Normal")
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
			}
        }

        response {
            clz APICreateAccountEvent.class
        }
    }
}