package org.zstack.ldap

import org.zstack.ldap.APICreateLdapBindingEvent

doc {
    title "CreateLdapBinding"

    category "ldap"

    desc """创建LDAP绑定"""

    rest {
        request {
			url "POST /v1/ldap/bindings"

			header (Authorization: 'OAuth the-session-uuid')

            clz APICreateLdapBindingMsg.class

            desc """创建LDAP绑定"""
            
			params {

				column {
					name "ldapUid"
					enclosedIn "params"
					desc "LDAP UID"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "accountUuid"
					enclosedIn "params"
					desc "账户UUID"
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
            clz APICreateLdapBindingEvent.class
        }
    }
}