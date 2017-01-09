package org.zstack.header.identity

org.zstack.header.identity.APIQuerySharedResourceReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QuerySharedResource"

    category "identity"

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