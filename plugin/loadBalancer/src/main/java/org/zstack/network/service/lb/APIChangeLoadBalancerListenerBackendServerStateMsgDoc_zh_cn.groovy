package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIChangeLoadBalancerListenerBackendServerStateEvent

doc {
    title "ChangeLoadBalancerListenerBackendServerState"

    category "loadBalancer"

    desc """修改负载均衡监听器下后端服务器的启用状态"""

    rest {
        request {
			url "PUT /v1/load-balancers/listeners/{listenerUuid}/servergroups/{serverGroupUuid}/backendservers/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIChangeLoadBalancerListenerBackendServerStateMsg.class

            desc """"""
            
			params {

				column {
					name "listenerUuid"
					enclosedIn "changeLoadBalancerListenerBackendServerState"
					desc "负载均衡监听器UUID"
					location "url"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "serverGroupUuid"
					enclosedIn "changeLoadBalancerListenerBackendServerState"
					desc "后端服务器组UUID"
					location "url"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "vmNicUuids"
					enclosedIn "changeLoadBalancerListenerBackendServerState"
					desc "当前后端服务器组中需要启用或停用的云主机网卡UUID列表"
					location "body"
					type "List"
					optional true
					since "5.5.38"
				}
				column {
					name "serverIps"
					enclosedIn "changeLoadBalancerListenerBackendServerState"
					desc "当前后端服务器组中需要启用或停用的IP地址列表；仅性能独享型负载均衡支持，且与vmNicUuids至少一个非空"
					location "body"
					type "List"
					optional true
					since "5.5.38"
				}
				column {
					name "state"
					enclosedIn "changeLoadBalancerListenerBackendServerState"
					desc "本次批量操作的目标管理态"
					location "body"
					type "String"
					optional false
					since "5.5.38"
					values ("Enabled","Disabled")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.5.38"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.5.38"
				}
			}
        }

        response {
            clz APIChangeLoadBalancerListenerBackendServerStateEvent.class
        }
    }
}