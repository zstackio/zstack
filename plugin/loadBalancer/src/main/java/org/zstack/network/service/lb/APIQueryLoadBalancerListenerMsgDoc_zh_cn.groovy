package org.zstack.network.service.lb

import org.zstack.header.query.APIQueryMessage

doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/load-balancers/listeners"

			url "GET /v1/load-balancers/listeners/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryLoadBalancerListenerMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryLoadBalancerListenerReply.class
        }
    }
}