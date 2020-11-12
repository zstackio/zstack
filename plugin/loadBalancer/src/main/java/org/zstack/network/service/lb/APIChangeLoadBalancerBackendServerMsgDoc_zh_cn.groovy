package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIChangeLoadBalancerBackendServerEvent

doc {
    title "ChangeLoadBalancerBackendServer"

    category "未知类别"

    desc """在这里填写API描述"""

    rest {
        request {
			url "PUT /v1/load-balancers/servergroup/{uuid}/backendserver/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIChangeLoadBalancerBackendServerMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn "changeLoadBalancerBackendServer"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "vmNics"
					enclosedIn "changeLoadBalancerBackendServer"
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "serverIps"
					enclosedIn "changeLoadBalancerBackendServer"
					desc ""
					location "body"
					type "List"
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
            clz APIChangeLoadBalancerBackendServerEvent.class
        }
    }
}