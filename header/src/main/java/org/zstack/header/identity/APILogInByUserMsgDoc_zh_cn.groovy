package org.zstack.header.identity

import org.zstack.header.identity.APILogInReply

doc {
    title "LogInByUser"

    category "identity"

    desc """使用用户身份登录"""

    rest {
        request {
			url "PUT /v1/accounts/users/login"



            clz APILogInByUserMsg.class

            desc """使用用户身份登录"""
            
			params {

				column {
					name "accountUuid"
					enclosedIn "logInByUser"
					desc "账户UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "accountName"
					enclosedIn "logInByUser"
					desc "账户名称"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "userName"
					enclosedIn "logInByUser"
					desc "用户名称"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "password"
					enclosedIn "logInByUser"
					desc "密码"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "clientInfo"
					enclosedIn "logInByUser"
					desc "客户端信息"
					location "body"
					type "Map"
					optional true
					since "3.5.0"
					
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