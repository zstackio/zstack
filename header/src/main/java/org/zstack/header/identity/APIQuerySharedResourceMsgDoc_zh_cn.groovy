package org.zstack.header.identity

import org.zstack.header.identity.APIQuerySharedResourceReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QuerySharedResource"

    category "identity"

    desc """查询共享资源"""

    rest {
        request {
			url "GET /v1/accounts/resources"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIQuerySharedResourceMsg.class

            desc """查询共享资源"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQuerySharedResourceReply.class
        }
    }
}