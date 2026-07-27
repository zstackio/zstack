package org.zstack.networksecuritypolicyschedule

import org.zstack.networksecuritypolicyschedule.APIGetNetworkSecurityPolicyScheduleReply
import org.zstack.header.message.APISyncCallMessage

doc {
    title "GetNetworkSecurityPolicySchedule"

    category "networkSecurityPolicySchedule"

    desc """获取资源的安全策略定时计划"""

    rest {
        request {
			url "GET /v1/network-security-policy-schedules"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIGetNetworkSecurityPolicyScheduleMsg.class

            desc """null"""
            
			params APISyncCallMessage.class
        }

        response {
            clz APIGetNetworkSecurityPolicyScheduleReply.class
        }
    }
}