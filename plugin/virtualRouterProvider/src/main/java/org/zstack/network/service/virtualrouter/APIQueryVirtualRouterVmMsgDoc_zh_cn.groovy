package org.zstack.network.service.virtualrouter

import org.zstack.header.query.APIQueryMessage

doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/vm-instances/appliances/virtual-routers"

			url "GET /v1/vm-instances/appliances/virtual-routers/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryVirtualRouterVmMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryApplianceVmReply.class
        }
    }
}