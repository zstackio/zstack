package org.zstack.header.network.l3

import org.zstack.header.query.APIQueryMessage

doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/l3-networks"

			url "GET /v1/l3-networks/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryL3NetworkMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryL3NetworkReply.class
        }
    }
}