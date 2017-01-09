package org.zstack.header.identity

org.zstack.header.identity.APIQueryUserGroupReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryUserGroup"

    category "identity"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/accounts/groups"

			url "GET /v1/accounts/groups/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryUserGroupMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryUserGroupReply.class
        }
    }
}