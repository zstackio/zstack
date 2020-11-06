package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIQueryLoadBalancerServerGroupReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "查询负载均衡服务器"

    category "负载均衡器"

    desc """查询已有的服务器组"""

    rest {
        request {
			url "GET /v1/load-balancers/servergroups"
			url "GET /v1/load-balancers/servergroups/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIQueryLoadBalancerServerGroupMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryLoadBalancerServerGroupReply.class
        }
    }
}