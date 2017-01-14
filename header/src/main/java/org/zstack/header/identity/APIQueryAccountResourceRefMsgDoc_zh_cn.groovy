package org.zstack.header.identity

import org.zstack.header.identity.APIQueryAccountResourceRefReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryAccountResourceRef"

    category "identity"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/accounts/resources/refs"


            header (OAuth: 'the-session-uuid')

            clz APIQueryAccountResourceRefMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryAccountResourceRefReply.class
        }
    }
}