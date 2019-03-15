package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIChangeLoadBalancerListenerEvent

doc {
    title "ChangeLoadBalancerListener"

    category "loadBalancer"

    desc """修改负载均衡监听器参数"""

    rest {
        request {
			url "PUT /v1/load-balancers/listeners/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIChangeLoadBalancerListenerMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn "changeLoadBalancerListener"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.4"
					
				}
				column {
					name "connectionIdleTimeout"
					enclosedIn "changeLoadBalancerListener"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "3.4"
					
				}
				column {
					name "maxConnection"
					enclosedIn "changeLoadBalancerListener"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "3.4"
					
				}
				column {
					name "balancerAlgorithm"
					enclosedIn "changeLoadBalancerListener"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.4"
					values ("roundrobin","leastconn","source")
				}
				column {
					name "healthCheckTarget"
					enclosedIn "changeLoadBalancerListener"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.4"
					
				}
				column {
					name "healthyThreshold"
					enclosedIn "changeLoadBalancerListener"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "3.4"
					
				}
				column {
					name "unhealthyThreshold"
					enclosedIn "changeLoadBalancerListener"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "3.4"
					
				}
				column {
					name "healthCheckInterval"
					enclosedIn "changeLoadBalancerListener"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "3.4"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.4"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.4"
					
				}
			}
        }

        response {
            clz APIChangeLoadBalancerListenerEvent.class
        }
    }
}