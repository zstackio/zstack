package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIUpdateLoadBalancerServerGroupEvent

doc {
    title "更新负载均衡服务器组"

    category "loadBalancer"

    desc """更新负载均衡服务器组"""

    rest {
        request {
			url "PUT /v1/load-balancers/servergroups/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIUpdateLoadBalancerServerGroupMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn "updateLoadBalancerServerGroup"
					desc "服务器组UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "name"
					enclosedIn "updateLoadBalancerServerGroup"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "description"
					enclosedIn "updateLoadBalancerServerGroup"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "weight"
					enclosedIn "updateLoadBalancerServerGroup"
					desc "服务器组权重"
					location "body"
					type "Integer"
					optional true
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
            clz APIUpdateLoadBalancerServerGroupEvent.class
        }
    }
}