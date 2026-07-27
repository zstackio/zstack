package org.zstack.networksecuritypolicyschedule

import org.zstack.networksecuritypolicyschedule.APISetNetworkSecurityPolicyScheduleEvent

doc {
    title "SetNetworkSecurityPolicySchedule"

    category "networkSecurityPolicySchedule"

    desc """设置资源当前使用的安全策略定时计划；不传scheduleUuid时取消当前设置"""

    rest {
        request {
			url "PUT /v1/network-security-policy-schedules/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APISetNetworkSecurityPolicyScheduleMsg.class

            desc """null"""
            
			params {

				column {
					name "scheduleUuid"
					enclosedIn "setNetworkSecurityPolicySchedule"
					desc "定时计划UUID；为空时取消当前设置"
					location "body"
					type "String"
					optional true
					since "5.5.38"
				}
				column {
					name "resourceType"
					enclosedIn "setNetworkSecurityPolicySchedule"
					desc "目标资源类型"
					location "body"
					type "String"
					optional false
					since "5.5.38"
					values ("SecurityGroup","VpcFirewallRuleSet")
				}
				column {
					name "resourceUuid"
					enclosedIn "setNetworkSecurityPolicySchedule"
					desc "目标资源UUID"
					location "body"
					type "String"
					optional false
					since "5.5.38"
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
            clz APISetNetworkSecurityPolicyScheduleEvent.class
        }
    }
}