package org.zstack.header.server

import org.zstack.header.server.APIQueryPhysicalServerRoleReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryPhysicalServerRole"

    category "physicalServer"

    desc """在这里填写API描述"""

    rest {
        request {
			url "GET /v1/physical-server-roles"
			url "GET /v1/physical-server-roles/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIQueryPhysicalServerRoleMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryPhysicalServerRoleReply.class
        }
    }
}