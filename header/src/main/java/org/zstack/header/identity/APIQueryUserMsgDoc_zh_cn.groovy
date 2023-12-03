package org.zstack.header.identity

import org.zstack.header.identity.APIQueryUserReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryUser"

    category "identity"

    desc """查询用户"""

    rest {
        request {
			url "GET /v1/accounts/users"
			url "GET /v1/accounts/users/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIQueryUserMsg.class

            desc """查询用户"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryUserReply.class
        }
    }
}