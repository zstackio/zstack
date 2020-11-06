package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIRemoveBackendServerFromServerGroupEvent

doc {
    title "从服务器组移除服务器"

    category "负载均衡器"

    desc """从服务器组移除网卡或者服务器IP"""

    rest {
        request {
			url "PUT /v1/load-balancers/servergroups/{serverGroupUuid}/backendservers/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIRemoveBackendServerFromServerGroupMsg.class

            desc """"""
            
			params {

				column {
					name "serverGroupUuid"
					enclosedIn "removeBackendServerFromServerGroup"
					desc "服务器组UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "vmNicUuids"
					enclosedIn "removeBackendServerFromServerGroup"
					desc "虚拟机网卡UUID"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "serverIps"
					enclosedIn "removeBackendServerFromServerGroup"
					desc "服务器IP"
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
            clz APIRemoveBackendServerFromServerGroupEvent.class
        }
    }
}