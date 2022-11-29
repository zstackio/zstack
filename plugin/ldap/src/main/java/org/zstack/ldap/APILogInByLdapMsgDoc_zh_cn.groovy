package org.zstack.ldap

import org.zstack.ldap.APILogInByLdapReply

doc {
    title "LogInByLdap"

    category "ldap"

    desc """使用LDAP身份登录"""

    rest {
        request {
			url "PUT /v1/ldap/login"



            clz APILogInByLdapMsg.class

            desc """使用LDAP身份登录"""
            
			params {

				column {
					name "uid"
					enclosedIn "logInByLdap"
					desc "LDAP UID"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "password"
					enclosedIn "logInByLdap"
					desc "密码"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "verifyCode"
					enclosedIn "logInByLdap"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "captchaUuid"
					enclosedIn "logInByLdap"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "clientInfo"
					enclosedIn "logInByLdap"
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
            clz APILogInByLdapReply.class
        }
    }
}