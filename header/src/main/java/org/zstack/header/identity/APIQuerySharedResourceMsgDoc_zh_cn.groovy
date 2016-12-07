package org.zstack.header.identity

import org.zstack.header.query.APIQueryMessage

doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/accounts/resources"


            header (OAuth: 'the-session-uuid')

            clz APIQuerySharedResourceMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQuerySharedResourceReply.class
        }
    }
}