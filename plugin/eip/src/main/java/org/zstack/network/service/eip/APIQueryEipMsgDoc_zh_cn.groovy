package org.zstack.network.service.eip

import org.zstack.header.query.APIQueryMessage

doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/eips"

			url "GET /v1/eips/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryEipMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryEipReply.class
        }
    }
}