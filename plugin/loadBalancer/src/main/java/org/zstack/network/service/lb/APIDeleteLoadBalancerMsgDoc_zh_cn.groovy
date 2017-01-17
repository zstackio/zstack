package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIDeleteLoadBalancerEvent

doc {
    title "DeleteLoadBalancer"

    category "loadBalancer"

    desc "在这里填写API描述"

    rest {
        request {
			url "DELETE /v1/load-balancers/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIDeleteLoadBalancerMsg.class

            desc ""
            
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
					name "deleteMode"
 					enclosedIn ""
 					desc ""
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
            clz APIDeleteLoadBalancerEvent.class
        }
    }
}