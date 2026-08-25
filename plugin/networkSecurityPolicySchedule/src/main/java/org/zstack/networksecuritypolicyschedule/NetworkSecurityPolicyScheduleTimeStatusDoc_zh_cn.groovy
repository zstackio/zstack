package org.zstack.networksecuritypolicyschedule



doc {

	title "安全策略定时计划时间状态"

	field {
		name "NotStarted"
		desc "尚未到整体开始边界"
		type "NetworkSecurityPolicyScheduleTimeStatus"
		since "5.5.38"
	}
	field {
		name "InWindow"
		desc "当前分钟处于有效时间窗口"
		type "NetworkSecurityPolicyScheduleTimeStatus"
		since "5.5.38"
	}
	field {
		name "OutOfWindow"
		desc "当前不在有效时间窗口，但仍存在后续有效窗口"
		type "NetworkSecurityPolicyScheduleTimeStatus"
		since "5.5.38"
	}
	field {
		name "Ended"
		desc "已不存在后续有效窗口"
		type "NetworkSecurityPolicyScheduleTimeStatus"
		since "5.5.38"
	}
}
