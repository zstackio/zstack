package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIGetLoadBalancerServerGroupBackendServerReply

doc {
    title "GetLoadBalancerServerGroupBackendServer"

    category "loadBalancer"

    desc """查询负载均衡服务器组的后端服务器列表,支持按服务器名称模糊搜索、启用状态过滤以及按服务器名称等字段排序,名称与IP由后端关联云主机信息后返回"""

    rest {
        request {
			url "GET /v1/load-balancers/listeners/{listenerUuid}/backendservers"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIGetLoadBalancerServerGroupBackendServerMsg.class

            desc """"""
            
			params {

				column {
					name "listenerUuid"
					enclosedIn ""
					desc "监听器UUID,返回该监听器下所有服务器组的后端服务器"
					location "url"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "serverGroupUuid"
					enclosedIn ""
					desc "服务器组UUID,可选过滤,不填返回该监听器下所有服务器组的后端服务器"
					location "query"
					type "String"
					optional true
					since "5.5.38"
				}
				column {
					name "limit"
					enclosedIn ""
					desc "分页大小,默认1000"
					location "query"
					type "Integer"
					optional true
					since "5.5.38"
				}
				column {
					name "start"
					enclosedIn ""
					desc "分页起始偏移,默认0"
					location "query"
					type "Integer"
					optional true
					since "5.5.38"
				}
				column {
					name "name"
					enclosedIn ""
					desc "服务器名称,模糊匹配,不填返回全部"
					location "query"
					type "String"
					optional true
					since "5.5.38"
				}
				column {
					name "state"
					enclosedIn ""
					desc "启用状态过滤,Enabled为启用,Disabled为禁用,不填返回全部"
					location "query"
					type "String"
					optional true
					since "5.5.38"
					values ("Enabled","Disabled")
				}
				column {
					name "sortBy"
					enclosedIn ""
					desc "排序字段,默认createDate"
					location "query"
					type "String"
					optional true
					since "5.5.38"
					values ("serverName","ip","weight","state","createDate")
				}
				column {
					name "sortDirection"
					enclosedIn ""
					desc "排序方向,默认asc"
					location "query"
					type "String"
					optional true
					since "5.5.38"
					values ("asc","desc")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
        }

        response {
            clz APIGetLoadBalancerServerGroupBackendServerReply.class
        }
    }
}