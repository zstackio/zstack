package org.zstack.network.service.vip

import org.zstack.network.service.vip.APIQueryVipReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryVip"

    category "vip"

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