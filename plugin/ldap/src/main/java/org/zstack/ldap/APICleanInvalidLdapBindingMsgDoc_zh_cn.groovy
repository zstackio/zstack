package org.zstack.ldap

import org.zstack.ldap.APICleanInvalidLdapBindingEvent

doc {
    title "CleanInvalidLdapBinding"

    category "ldap"

    desc """清理无效的LDAP绑定"""

    rest {
        request {
			url "PUT /v1/ldap/bindings/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APICleanInvalidLdapBindingMsg.class

            desc """清理无效的LDAP绑定"""
            
			params {

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
            clz APICleanInvalidLdapBindingEvent.class
        }
    }
}