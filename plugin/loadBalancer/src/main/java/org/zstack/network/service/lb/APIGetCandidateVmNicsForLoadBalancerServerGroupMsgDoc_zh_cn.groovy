package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIGetCandidateVmNicsForLoadBalancerServerGroupReply

doc {
    title "GetCandidateVmNicsForLoadBalancerServerGroup"

    category "loadBalancer"

    desc """查询负载均衡后端服务器组可以添加网卡列表"""

    rest {
        request {
			url "GET /v1/load-balancers/servergroups/candidate-nics"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIGetCandidateVmNicsForLoadBalancerServerGroupMsg.class

            desc """"""
            
			params {

				column {
					name "servergroupUuid"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "loadBalancerUuid"
					enclosedIn ""
					desc "负载均衡器UUID"
					location "query"
					type "String"
					optional true
					since "0.6"
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
				column {
					name "ipVersion"
					enclosedIn ""
					desc ""
					location "query"
					type "Integer"
					optional true
					since "5.1.0"
					values ("4","6")
				}
			}
        }

        response {
            clz APIGetCandidateVmNicsForLoadBalancerServerGroupReply.class
        }
    }
}