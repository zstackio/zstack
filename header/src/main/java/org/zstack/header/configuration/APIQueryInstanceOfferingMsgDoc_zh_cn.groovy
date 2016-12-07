package org.zstack.header.configuration

import org.zstack.header.query.APIQueryMessage

doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/instance-offerings"

			url "GET /v1/instance-offerings/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryInstanceOfferingMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryInstanceOfferingReply.class
        }
    }
}