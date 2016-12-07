package org.zstack.network.service.lb



doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "PUT /v1/load-balancers/{uuid}/actions"


            header (OAuth: 'the-session-uuid')

            clz APIRefreshLoadBalancerMsg.class

            desc ""
            
			params {

				column {
					name "uuid"
					enclosedIn "refreshLoadBalancer"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIRefreshLoadBalancerEvent.class
        }
    }
}