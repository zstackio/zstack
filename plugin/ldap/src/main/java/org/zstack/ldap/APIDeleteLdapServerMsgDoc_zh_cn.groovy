package org.zstack.ldap

import org.zstack.ldap.APIDeleteLdapServerEvent

doc {
    title "DeleteLdapServer"

    category "ldap"

    desc """删除LDAP服务器"""

    rest {
        request {
			url "DELETE /v1/ldap/servers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIDeleteLdapServerMsg.class

            desc """删除LDAP服务器"""
            
			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式"
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
            clz APIDeleteLdapServerEvent.class
        }
    }
}