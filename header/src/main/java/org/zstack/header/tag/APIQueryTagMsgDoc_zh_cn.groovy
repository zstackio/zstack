package org.zstack.header.tag

import org.zstack.header.query.APIQueryMessage

doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/tags"

			url "GET /v1/tags/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryTagMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryTagReply.class
        }
    }
}