package org.zstack.network.service.lb

import org.zstack.network.service.lb.APICreateLoadBalancerListenerEvent

doc {
    title "创建负载均衡监听器(CreateLoadBalancerListener)"

    category "负载均衡"

    desc """创建负载均衡监听器"""

    rest {
        request {
			url "POST /v1/load-balancers/{loadBalancerUuid}/listeners"

			header (Authorization: 'OAuth the-session-uuid')


            clz APICreateLoadBalancerListenerMsg.class

            desc """"""
            
			params {

				column {
					name "loadBalancerUuid"
					enclosedIn "params"
					desc "负载均衡器UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "云主机端口"
					enclosedIn "params"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "0.6"
					
				}
				column {
					name "loadBalancerPort"
					enclosedIn "params"
					desc "负载均衡器端口"
					location "body"
					type "int"
					optional false
					since "0.6"
					
				}
				column {
					name "protocol"
					enclosedIn "params"
					desc "协议"
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("tcp","http")
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
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
            clz APICreateLoadBalancerListenerEvent.class
        }
    }
}