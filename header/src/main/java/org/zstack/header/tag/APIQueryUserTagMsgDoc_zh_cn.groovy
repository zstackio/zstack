package org.zstack.header.tag

import org.zstack.header.identity.APIQueryUserReply
import org.zstack.header.query.APIQueryMessage
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryUserTag"

    category "tag"

    desc """查询用户标签"""

    rest {
        request {
			url "GET /v1/user-tags"
			url "GET /v1/user-tags/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')


            clz APIQueryUserTagMsg.class

            desc """查询用户标签"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryUserReply.class
        }
    }
}