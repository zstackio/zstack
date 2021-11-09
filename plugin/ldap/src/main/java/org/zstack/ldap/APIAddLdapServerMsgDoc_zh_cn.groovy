package org.zstack.ldap

import org.zstack.ldap.APIAddLdapServerEvent

doc {
    title "AddLdapServer"

    category "ldap"

    desc """添加LDAP服务器"""

    rest {
        request {
			url "POST /v1/ldap/servers"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIAddLdapServerMsg.class

            desc """添加LDAP服务器"""
            
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
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "url"
					enclosedIn "params"
					desc "LDAP服务器访问地址"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "base"
					enclosedIn "params"
					desc "LDAP服务查询BaseDN"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "username"
					enclosedIn "params"
					desc "访问LDAP服务器使用的用户名"
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
					name "encryption"
					enclosedIn "params"
					desc "加密方式"
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("None","TLS")
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
					name "scope"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("account","IAM2")
				}
			}
        }

        response {
            clz APIAddLdapServerEvent.class
        }
    }
}