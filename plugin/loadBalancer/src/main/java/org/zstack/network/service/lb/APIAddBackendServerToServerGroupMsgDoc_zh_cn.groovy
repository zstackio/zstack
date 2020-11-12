package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIAddBackendServerToServerGroupEvent

doc {
    title "AddBackendServerToServerGroup"

    category "loadBalancer"

    desc """在这里填写API描述"""

    rest {
        request {
			url "POST /v1/load-balancers/servergroups/{serverGroupUuid}/backendservers"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIAddBackendServerToServerGroupMsg.class

            desc """"""
            
			params {

				column {
					name "serverGroupUuid"
					enclosedIn "params"
					desc ""
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "vmNics"
					enclosedIn "params"
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "servers"
					enclosedIn "params"
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
            clz APIAddBackendServerToServerGroupEvent.class
        }
    }
}