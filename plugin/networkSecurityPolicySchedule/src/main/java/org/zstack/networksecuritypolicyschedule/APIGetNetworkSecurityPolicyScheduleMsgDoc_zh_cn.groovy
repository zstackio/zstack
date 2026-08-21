package org.zstack.networksecuritypolicyschedule

import org.zstack.networksecuritypolicyschedule.APIGetNetworkSecurityPolicyScheduleReply

doc {
    title "GetNetworkSecurityPolicySchedule"

    category "networkSecurityPolicySchedule"

    desc """获取资源的安全策略定时计划"""

    rest {
        request {
			url "GET /v1/network-security-policy-schedules"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIGetNetworkSecurityPolicyScheduleMsg.class

            desc """"""
            
			params {

				column {
					name "resourceUuid"
					enclosedIn ""
					desc "资源UUID"
					location "query"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "5.5.38"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "5.5.38"
				}
			}
        }

        response {
            clz APIGetNetworkSecurityPolicyScheduleReply.class
        }
    }
}