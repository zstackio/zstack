package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIAddServerGroupToLoadBalancerListenerEvent

doc {
    title "添加服务器组到负载均衡监听器"

    category "负载均衡器"

    desc """添加一个服务器组到负载均衡监听器器"""

    rest {
        request {
			url "POST /v1/load-balancers/listeners/{listenerUuid}/servergroups"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIAddServerGroupToLoadBalancerListenerMsg.class

            desc """"""
            
			params {

				column {
					name "serverGroupUuid"
					enclosedIn "params"
					desc "服务器组UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "listenerUuid"
					enclosedIn "params"
					desc "监听器UUID"
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
            clz APIAddServerGroupToLoadBalancerListenerEvent.class
        }
    }
}