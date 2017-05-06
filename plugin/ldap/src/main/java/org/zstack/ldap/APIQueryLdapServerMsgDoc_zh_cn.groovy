package org.zstack.ldap

import org.zstack.ldap.APIQueryLdapServerReply
import org.zstack.header.query.APIQueryMessage
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryLdapServer"

    category "ldap"

    desc """查询LDAP服务器"""

    rest {
        request {
			url "GET /v1/ldap/servers"
			url "GET /v1/ldap/servers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')


            clz APIQueryLdapServerMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryLdapServerReply.class
        }
    }
}