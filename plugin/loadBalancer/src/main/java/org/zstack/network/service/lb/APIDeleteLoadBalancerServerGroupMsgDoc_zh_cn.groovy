package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIDeleteLoadBalancerServerGroupEvent

doc {
    title "删除负载均衡服务器组"

    category "负载均衡器"

    desc """删除一个负载均衡服务器组"""

    rest {
        request {
			url "DELETE /v1/load-balancers/servergroups/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIDeleteLoadBalancerServerGroupMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIDeleteLoadBalancerServerGroupEvent.class
        }
    }
}