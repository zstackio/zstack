package org.zstack.networksecuritypolicyschedule

import org.zstack.networksecuritypolicyschedule.APIUpdateNetworkSecurityPolicyScheduleEvent

doc {
    title "UpdateNetworkSecurityPolicySchedule"

    category "networkSecurityPolicySchedule"

    desc """全量修改安全策略定时计划"""

    rest {
        request {
			url "PUT /v1/network-security-policy-schedules/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIUpdateNetworkSecurityPolicyScheduleMsg.class

            desc """null"""
            
			params {

				column {
					name "uuid"
					enclosedIn "updateNetworkSecurityPolicySchedule"
					desc "定时计划UUID"
					location "url"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "name"
					enclosedIn "updateNetworkSecurityPolicySchedule"
					desc "名称"
					location "body"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "description"
					enclosedIn "updateNetworkSecurityPolicySchedule"
					desc "描述；不传则清空"
					location "body"
					type "String"
					optional true
					since "5.5.38"
				}
				column {
					name "timeType"
					enclosedIn "updateNetworkSecurityPolicySchedule"
					desc "时间类型，Local 或 UTC"
					location "body"
					type "String"
					optional false
					since "5.5.38"
					values ("Local","UTC")
				}
				column {
					name "repeatType"
					enclosedIn "updateNetworkSecurityPolicySchedule"
					desc "计划类型"
					location "body"
					type "String"
					optional false
					since "5.5.38"
					values ("Once","Weekly")
				}
				column {
					name "startDate"
					enclosedIn "updateNetworkSecurityPolicySchedule"
					desc "开始日期，格式yyyy-MM-dd"
					location "body"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "endDate"
					enclosedIn "updateNetworkSecurityPolicySchedule"
					desc "结束日期，格式yyyy-MM-dd"
					location "body"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "startTime"
					enclosedIn "updateNetworkSecurityPolicySchedule"
					desc "开始时间，格式HH:mm"
					location "body"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "endTime"
					enclosedIn "updateNetworkSecurityPolicySchedule"
					desc "结束时间，格式HH:mm；Weekly使用00:00至00:00表示全天"
					location "body"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "weekDays"
					enclosedIn "updateNetworkSecurityPolicySchedule"
					desc "Weekly生效星期，1表示周一，7表示周日"
					location "body"
					type "List"
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
            clz APIUpdateNetworkSecurityPolicyScheduleEvent.class
        }
    }
}