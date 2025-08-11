package org.zstack.header.identity

import org.zstack.header.identity.APIQueryResourceResponsibleReply
import org.zstack.header.query.APIQueryMessage
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryResourceResponsible"

    category "identity"

    desc """查询资源责任关系"""

    rest {
        request {
			url "GET /v1/resources/responsible"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIQueryResourceResponsibleMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryResourceResponsibleReply.class
        }
    }
}