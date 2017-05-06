package org.zstack.ldap

import org.zstack.ldap.APIUpdateLdapServerEvent

doc {
    title "UpdateLdapServer"

    category "ldap"

    desc """更新LDAP服务器"""

    rest {
        request {
			url "PUT /v1/ldap/servers/{ldapServerUuid}"

			header (Authorization: 'OAuth the-session-uuid')


            clz APIUpdateLdapServerMsg.class

            desc """更新LDAP服务器"""
            
			params {

				column {
					name "ldapServerUuid"
					enclosedIn "updateLdapServer"
					desc "LDAP服务器的UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "name"
					enclosedIn "updateLdapServer"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "description"
					enclosedIn "updateLdapServer"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "url"
					enclosedIn "updateLdapServer"
					desc "LDAP服务器的访问地址"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "base"
					enclosedIn "updateLdapServer"
					desc "LDAP服务器的查询BaseDN"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "username"
					enclosedIn "updateLdapServer"
					desc "访问LDAP服务器的用户名"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "password"
					enclosedIn "updateLdapServer"
					desc "密码"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "encryption"
					enclosedIn "updateLdapServer"
					desc "加密方式"
					location "body"
					type "String"
					optional true
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
			}
        }

        response {
            clz APIUpdateLdapServerEvent.class
        }
    }
}