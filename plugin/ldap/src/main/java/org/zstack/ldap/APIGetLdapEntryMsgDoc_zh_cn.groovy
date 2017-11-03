package org.zstack.ldap

import org.zstack.ldap.APIGetLdapEntryReply

doc {
    title "GetLdapEntry"

    category "ldap"

    desc """查询Ldap条目"""

    rest {
        request {
			url "GET /v1/ldap/entry"


            header(Authorization: 'OAuth the-session-uuid')

            clz APIGetLdapEntryMsg.class

            desc """"""
            
			params {

				column {
					name "ldapFilter"
					enclosedIn ""
					desc "ldap查询条件"
					location "query"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIGetLdapEntryReply.class
        }
    }
}