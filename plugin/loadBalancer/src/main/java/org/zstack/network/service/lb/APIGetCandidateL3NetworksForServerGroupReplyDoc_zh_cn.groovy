package org.zstack.network.service.lb

import org.zstack.header.network.l3.L3NetworkInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "在这里输入结构的名称"

	ref {
		name "inventories"
		path "org.zstack.network.service.lb.APIGetCandidateL3NetworksForServerGroupReply.inventories"
		desc "null"
		type "List"
		since "4.3.0"
		clz L3NetworkInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.3.0"
	}
	ref {
		name "error"
		path "org.zstack.network.service.lb.APIGetCandidateL3NetworksForServerGroupReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.3.0"
		clz ErrorCode.class
	}
}
