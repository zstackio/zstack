package org.zstack.header.network.service

import org.zstack.header.query.APIQueryMessage

doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/network-services/providers"


            header (OAuth: 'the-session-uuid')

            clz APIQueryNetworkServiceProviderMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryNetworkServiceProviderReply.class
        }
    }
}