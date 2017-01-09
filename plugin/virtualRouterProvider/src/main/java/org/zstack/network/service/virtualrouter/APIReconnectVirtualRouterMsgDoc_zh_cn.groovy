package org.zstack.network.service.virtualrouter

import org.zstack.network.service.virtualrouter.APIReconnectVirtualRouterEvent

doc {
    title "ReconnectVirtualRouter"

    category "virtualRouter"

    desc "在这里填写API描述"

    rest {
        request {
			url "PUT /v1/vm-instances/appliances/virtual-routers/{vmInstanceUuid}/actions"


            header (OAuth: 'the-session-uuid')

            clz APIReconnectVirtualRouterMsg.class

            desc ""
            
			params {

				column {
					name "vmInstanceUuid"
					enclosedIn "reconnectVirtualRouter"
					desc "云主机UUID"
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
            clz APIReconnectVirtualRouterEvent.class
        }
    }
}