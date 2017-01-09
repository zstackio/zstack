package org.zstack.header.tag

import org.zstack.header.identity.APIQueryUserReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryUserTag"

    category "tag"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/user-tags"

			url "GET /v1/user-tags/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryUserTagMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryUserReply.class
        }
    }
}