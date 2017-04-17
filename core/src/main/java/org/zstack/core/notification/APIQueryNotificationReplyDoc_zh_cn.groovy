package org.zstack.core.notification

import org.zstack.header.errorcode.ErrorCode
import org.zstack.core.notification.NotificationInventory

doc {

	title "在这里输入结构的名称"

	ref {
		name "error"
		path "org.zstack.core.notification.APIQueryNotificationReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.core.notification.APIQueryNotificationReply.inventories"
		desc "null"
		type "List"
		since "0.6"
		clz NotificationInventory.class
	}
}
