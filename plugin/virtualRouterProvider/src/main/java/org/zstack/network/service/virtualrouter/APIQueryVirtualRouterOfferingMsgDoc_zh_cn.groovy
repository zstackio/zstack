package org.zstack.network.service.virtualrouter

import org.zstack.network.service.virtualrouter.APIQueryVirtualRouterOfferingReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryVirtualRouterOffering"

    category "virtualRouter"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/instance-offerings/virtual-routers"

			url "GET /v1/instance-offerings/virtual-routers/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryVirtualRouterOfferingMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryVirtualRouterOfferingReply.class
        }
    }
}