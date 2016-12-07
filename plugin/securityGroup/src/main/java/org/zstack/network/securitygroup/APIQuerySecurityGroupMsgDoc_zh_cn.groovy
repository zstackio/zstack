package org.zstack.network.securitygroup

import org.zstack.header.query.APIQueryMessage

doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/security-groups"

			url "GET /v1/security-groups/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQuerySecurityGroupMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQuerySecurityGroupReply.class
        }
    }
}