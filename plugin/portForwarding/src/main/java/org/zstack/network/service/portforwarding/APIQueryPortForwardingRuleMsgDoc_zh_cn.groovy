package org.zstack.network.service.portforwarding

import org.zstack.header.query.APIQueryMessage

doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/port-forwarding"

			url "GET /v1/port-forwarding/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryPortForwardingRuleMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryPortForwardingRuleReply.class
        }
    }
}