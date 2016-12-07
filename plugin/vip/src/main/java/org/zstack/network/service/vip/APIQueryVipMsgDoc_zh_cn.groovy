package org.zstack.network.service.vip

import org.zstack.header.query.APIQueryMessage

doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/vips"

			url "GET /v1/vips/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryVipMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryVipReply.class
        }
    }
}