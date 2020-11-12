package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIChangeLoadBalancerBackendServerEvent

doc {
    title "ChangeLoadBalancerBackendServer"

    category "loadBalancer"

    desc """在这里填写API描述"""

    rest {
        request {
			url "PUT /v1/load-balancers/servergroups/{serverGroupUuid}/backendserver/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIChangeLoadBalancerBackendServerMsg.class

            desc """"""
            
			params {

				column {
					name "serverGroupUuid"
					enclosedIn "changeLoadBalancerBackendServer"
					desc ""
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
					name "servers"
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