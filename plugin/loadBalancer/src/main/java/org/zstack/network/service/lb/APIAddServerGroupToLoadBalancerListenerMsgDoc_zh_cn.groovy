package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIAddServerGroupToLoadBalancerListenerEvent

doc {
    title "AddServerGroupToLoadBalancerListener"

    category "loadBalancer"

    desc """在这里填写API描述"""

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
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "listenerUuid"
					enclosedIn "params"
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
            clz APIAddServerGroupToLoadBalancerListenerEvent.class
        }
    }
}