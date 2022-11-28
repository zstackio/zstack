package org.zstack.ldap

import org.zstack.ldap.APIGetLdapEntryReply

doc {
    title "GetCandidateLdapEntryForBinding"

    category "ldap"

    desc """查询可绑定的LDAP/AD条目(排除已绑定的LDAP/AD用户)"""

    rest {
        request {
			url "GET /v1/ldap/entries/candidates"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIGetCandidateLdapEntryForBindingMsg.class

            desc """"""
            
			params {

				column {
					name "ldapFilter"
					enclosedIn ""
					desc "查询条件"
					location "query"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "limit"
					enclosedIn ""
					desc "最多返回的记录数，类似MySQL的limit"
					location "query"
					type "Integer"
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