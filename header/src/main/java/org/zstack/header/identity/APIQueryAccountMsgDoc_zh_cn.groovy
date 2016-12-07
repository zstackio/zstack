package org.zstack.header.identity

import org.zstack.header.query.APIQueryMessage

doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/accounts"

			url "GET /v1/accounts/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryAccountMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryAccountReply.class
        }
    }
}