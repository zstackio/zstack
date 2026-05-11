package org.zstack.header.server

import org.zstack.header.server.APIQueryServerPoolReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryServerPool"

    category "physicalServer"

    desc """在这里填写API描述"""

    rest {
        request {
			url "GET /v1/server-pools"
			url "GET /v1/server-pools/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIQueryServerPoolMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryServerPoolReply.class
        }
    }
}