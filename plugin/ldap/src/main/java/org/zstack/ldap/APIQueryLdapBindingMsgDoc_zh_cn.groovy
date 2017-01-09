package org.zstack.ldap

import org.zstack.ldap.APIQueryLdapBindingReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryLdapBinding"

    category "ldap"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/ldap/bindings"

			url "GET /v1/ldap/bindings/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryLdapBindingMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryLdapBindingReply.class
        }
    }
}