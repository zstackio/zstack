package org.zstack.header.server

import org.zstack.header.server.APIQueryPhysicalServerReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryPhysicalServer"

    category "physicalServer"

    desc """在这里填写API描述"""

    rest {
        request {
			url "GET /v1/physical-servers"
			url "GET /v1/physical-servers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIQueryPhysicalServerMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryPhysicalServerReply.class
        }
    }
}