package org.zstack.networksecuritypolicyschedule

import org.zstack.networksecuritypolicyschedule.APICreateNetworkSecurityPolicyScheduleEvent

doc {
    title "CreateNetworkSecurityPolicySchedule"

    category "networkSecurityPolicySchedule"

    desc """创建安全策略定时计划"""

    rest {
        request {
			url "POST /v1/network-security-policy-schedules"

			header (Authorization: 'OAuth the-session-uuid')

            clz APICreateNetworkSecurityPolicyScheduleMsg.class

            desc """null"""
            
			params {

				column {
					name "name"
					enclosedIn "params"
					desc "名称"
					location "body"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "描述"
					location "body"
					type "String"
					optional true
					since "5.5.38"
				}
				column {
					name "resourceType"
					enclosedIn "params"
					desc "所属资源类型"
					location "body"
					type "String"
					optional false
					since "5.5.38"
					values ("SecurityGroup","VpcFirewallRuleSet")
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "所属资源UUID"
					location "body"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "timeType"
					enclosedIn "params"
					desc "时间类型，Local 或 UTC"
					location "body"
					type "String"
					optional false
					since "5.5.38"
					values ("Local","UTC")
				}
				column {
					name "repeatType"
					enclosedIn "params"
					desc "计划类型"
					location "body"
					type "String"
					optional false
					since "5.5.38"
					values ("Once","Weekly")
				}
				column {
					name "startDate"
					enclosedIn "params"
					desc "开始日期，格式yyyy-MM-dd"
					location "body"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "endDate"
					enclosedIn "params"
					desc "结束日期，格式yyyy-MM-dd"
					location "body"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "startTime"
					enclosedIn "params"
					desc "开始时间，格式HH:mm"
					location "body"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "endTime"
					enclosedIn "params"
					desc "结束时间，格式HH:mm；Weekly使用00:00至00:00表示全天"
					location "body"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "weekDays"
					enclosedIn "params"
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
            clz APICreateNetworkSecurityPolicyScheduleEvent.class
        }
    }
}