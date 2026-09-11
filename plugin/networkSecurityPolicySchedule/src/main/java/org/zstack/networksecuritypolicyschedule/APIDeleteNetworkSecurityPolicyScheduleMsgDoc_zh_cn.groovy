package org.zstack.networksecuritypolicyschedule

import org.zstack.networksecuritypolicyschedule.APIDeleteNetworkSecurityPolicyScheduleEvent

doc {
    title "DeleteNetworkSecurityPolicySchedule"

    category "networkSecurityPolicySchedule"

    desc """删除安全策略定时计划"""

    rest {
        request {
			url "DELETE /v1/network-security-policy-schedules/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIDeleteNetworkSecurityPolicyScheduleMsg.class

            desc """null"""
            
			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "定时计划UUID"
					location "url"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式"
					location "body"
					type "String"
					optional true
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
            clz APIDeleteNetworkSecurityPolicyScheduleEvent.class
        }
    }
}