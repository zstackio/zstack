package org.zstack.header.host

import org.zstack.header.query.APIQueryMessage

doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/hosts"

			url "GET /v1/hosts/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryHostMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryHostReply.class
        }
    }
}