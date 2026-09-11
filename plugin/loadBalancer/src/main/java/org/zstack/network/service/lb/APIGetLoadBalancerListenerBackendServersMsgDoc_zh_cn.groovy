package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIGetLoadBalancerListenerBackendServersReply

doc {
    title "GetLoadBalancerListenerBackendServers"

    category "loadBalancer"

    desc """查询指定监听器和后端服务器组中的后端服务器状态"""

    rest {
        request {
			url "GET /v1/load-balancers/listeners/{listenerUuid}/servergroups/{serverGroupUuid}/backendservers"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIGetLoadBalancerListenerBackendServersMsg.class

            desc """"""
            
			params {

				column {
					name "listenerUuid"
					enclosedIn ""
					desc "负载均衡监听器UUID"
					location "url"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "serverGroupUuid"
					enclosedIn ""
					desc "后端服务器组UUID"
					location "url"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "5.5.38"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "5.5.38"
				}
			}
        }

        response {
            clz APIGetLoadBalancerListenerBackendServersReply.class
        }
    }
}