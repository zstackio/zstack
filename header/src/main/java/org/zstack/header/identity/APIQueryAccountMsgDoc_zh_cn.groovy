package org.zstack.header.identity

import org.zstack.header.identity.APIQueryAccountReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryAccount"

    category "identity"

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