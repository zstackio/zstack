package org.zstack.header.storage.primary

import org.zstack.header.storage.primary.UsageReport
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取主存储预测容量报告返回"

	ref {
		name "uriUsageForecast"
		path "org.zstack.header.storage.primary.APIGetPrimaryStorageUsageReportReply.uriUsageForecast"
		desc "null"
		type "Map"
		since "4.7.21"
		clz UsageReport.class
	}
	ref {
		name "usageReport"
		path "org.zstack.header.storage.primary.APIGetPrimaryStorageUsageReportReply.usageReport"
		desc "null"
		type "UsageReport"
		since "4.7.21"
		clz UsageReport.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.7.21"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.primary.APIGetPrimaryStorageUsageReportReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.7.21"
		clz ErrorCode.class
	}
}
