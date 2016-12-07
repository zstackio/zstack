package org.zstack.header.zone

import org.zstack.header.query.APIQueryMessage

doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/zones"

			url "GET /v1/zones/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryZoneMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryZoneReply.class
        }
    }
}