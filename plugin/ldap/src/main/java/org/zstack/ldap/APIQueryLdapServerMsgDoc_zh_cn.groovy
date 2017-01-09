package org.zstack.ldap

org.zstack.ldap.APIQueryLdapServerReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryLdapServer"

    category "ldap"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/ldap/servers"

			url "GET /v1/ldap/servers/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryLdapServerMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryLdapServerReply.class
        }
    }
}