package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIGetLoadBalancerListenerACLEntriesReply

doc {
    title "GetLoadBalancerListenerACLEntries"

    category "loadBalancer"

    desc """获取负载均衡监听器访问控制策略条目"""

    rest {
        request {
			url "GET /v1/load-balancers/listeners/access-control-lists/entries"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIGetLoadBalancerListenerACLEntriesMsg.class

            desc """"""
            
			params {

				column {
					name "listenerUuids"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "4.1.3"
					
				}
				column {
					name "type"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "4.1.3"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "4.1.3"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "4.1.3"
					
				}
			}
        }

        response {
            clz APIGetLoadBalancerListenerACLEntriesReply.class
        }
    }
}