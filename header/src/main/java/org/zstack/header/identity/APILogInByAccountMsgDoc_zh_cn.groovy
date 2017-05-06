package org.zstack.header.identity

import org.zstack.header.identity.APILogInReply

doc {
    title "LogInByAccount"

    category "identity"

    desc """使用账户身份登录"""

    rest {
        request {
			url "PUT /v1/accounts/login"




            clz APILogInByAccountMsg.class

            desc """使用账户身份登录"""
            
			params {

				column {
					name "accountName"
					enclosedIn "logInByAccount"
					desc "账户名称"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "password"
					enclosedIn "logInByAccount"
					desc "密码"
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
            clz APILogInReply.class
        }
    }
}