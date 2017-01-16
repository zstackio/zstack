package org.zstack.header.identity

import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryPolicy"

    category "identity"

    desc "查询策略"

    rest {
        request {
            url "GET /v1/accounts/policies"

            url "GET /v1/accounts/policies/{uuid}"


            header(OAuth: 'the-session-uuid')

            clz APIQueryPolicyMsg.class

            desc "查询策略"

            params APIQueryMessage.class
        }

        response {
            clz APIQueryPolicyReply.class
        }
    }
}