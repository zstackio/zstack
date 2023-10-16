package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIAttachVipToLoadBalancerEvent

doc {
    title "AttachVipToLoadBalancer"

    category "负载均衡"

    desc """加载VIP到负载均衡器"""

    rest {
        request {
			url "POST /v1/load-balancers/{loadBalancerUuid}/vip/{vipUuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIAttachVipToLoadBalancerMsg.class

            desc """"""
            
			params {

				column {
					name "loadBalancerUuid"
					enclosedIn "params"
					desc "负载均衡器UUID"
					location "url"
					type "String"
					optional false
					since "5.0.0"
				}
				column {
					name "vipUuid"
					enclosedIn "params"
					desc "VIP UUID"
					location "url"
					type "String"
					optional false
					since "5.0.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.0.0"
				}
			}
        }

        response {
            clz APIAttachVipToLoadBalancerEvent.class
        }
    }
}