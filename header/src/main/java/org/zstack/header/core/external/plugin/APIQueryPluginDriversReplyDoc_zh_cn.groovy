package org.zstack.header.core.external.plugin

import org.zstack.header.core.external.plugin.PluginDriverInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询插件驱动器"

	ref {
		name "inventories"
		path "org.zstack.header.core.external.plugin.APIQueryPluginDriversReply.inventories"
		desc "插件驱动器列表"
		type "List"
		since "5.3.0"
		clz PluginDriverInventory.class
	}
	field {
		name "success"
		desc "操作是否成功"
		type "boolean"
		since "5.3.0"
	}
	ref {
		name "error"
		path "org.zstack.header.core.external.plugin.APIQueryPluginDriversReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.3.0"
		clz ErrorCode.class
	}
}
