package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIRemoveServerGroupFromLoadBalancerListenerEvent

doc {
    title "从负载均衡监听器移除服务器组"

    category "负载均衡器"

    desc """从负载均衡器移除一个服务器组"""

    rest {
        request {
			url "DELETE /v1/load-balancers/listeners/{listenerUuid}/servergroups"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIRemoveServerGroupFromLoadBalancerListenerMsg.class

            desc """"""
            
			params {

				column {
					name "serverGroupUuid"
					enclosedIn ""
					desc "服务器组UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "listenerUuid"
					enclosedIn ""
					desc ""
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
            clz APIRemoveServerGroupFromLoadBalancerListenerEvent.class
        }
    }
}