package org.zstack.scheduler

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.core.scheduler.SchedulerInventory

doc {

	title "定时任务清单"

	ref {
		name "error"
		path "org.zstack.scheduler.APIChangeSchedulerStateEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.scheduler.APIChangeSchedulerStateEvent.inventory"
		desc "null"
		type "SchedulerInventory"
		since "0.6"
		clz SchedulerInventory.class
	}
}
