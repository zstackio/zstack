package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIAddBackendServerToServerGroupEvent

doc {
    title "添加后端服务器到服务器组"

    category "负载均衡器"

    desc """添加服务器网卡或者IP地址到服务器组"""

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
					desc "服务器组UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "vmNicUuids"
					enclosedIn "params"
					desc "虚拟机网卡UUID列表"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "serverIps"
					enclosedIn "params"
					desc "服务器IP地址列表"
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