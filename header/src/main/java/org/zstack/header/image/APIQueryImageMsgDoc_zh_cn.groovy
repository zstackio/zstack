package org.zstack.header.image

import org.zstack.header.query.APIQueryMessage

doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/images"

			url "GET /v1/images/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryImageMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryImageReply.class
        }
    }
}