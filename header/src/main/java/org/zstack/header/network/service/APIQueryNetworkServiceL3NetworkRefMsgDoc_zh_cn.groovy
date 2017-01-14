package org.zstack.header.network.service

import org.zstack.header.network.service.APIQueryNetworkServiceL3NetworkRefReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryNetworkServiceL3NetworkRef"

    category "network.l3"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/l3-networks/network-services/refs"


            header (OAuth: 'the-session-uuid')

            clz APIQueryNetworkServiceL3NetworkRefMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryNetworkServiceL3NetworkRefReply.class
        }
    }
}