package org.zstack.header.identity

import org.zstack.header.query.APIQueryMessage

doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/accounts/policies"

			url "GET /v1/accounts/policies/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryPolicyMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryPolicyReply.class
        }
    }
}