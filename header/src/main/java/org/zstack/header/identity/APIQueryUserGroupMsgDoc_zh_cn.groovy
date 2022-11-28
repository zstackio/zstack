package org.zstack.header.identity

import org.zstack.header.identity.APIQueryUserGroupReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryUserGroup"

    category "identity"

    desc """查询用户组"""

    rest {
        request {
			url "GET /v1/accounts/groups"
			url "GET /v1/accounts/groups/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIQueryUserGroupMsg.class

            desc """查询用户组"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryUserGroupReply.class
        }
    }
}