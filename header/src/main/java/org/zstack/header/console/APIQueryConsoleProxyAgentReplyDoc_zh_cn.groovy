package org.zstack.header.console

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.console.ConsoleProxyAgentInventory

doc {

	title "在这里输入结构的名称"

	ref {
		name "error"
		path "org.zstack.header.console.APIQueryConsoleProxyAgentReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.console.APIQueryConsoleProxyAgentReply.inventories"
		desc "null"
		type "List"
		since "0.6"
		clz ConsoleProxyAgentInventory.class
	}
}
