package org.zstack.network.service.vip

import org.zstack.network.service.vip.GetVipFreePortInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取VIP空闲端口"

	ref {
		name "inventory"
		path "org.zstack.network.service.vip.APIGetVipFreePortReply.inventory"
		desc "null"
		type "GetVipFreePortInventory"
		since "4.7.11"
		clz GetVipFreePortInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.7.11"
	}
	ref {
		name "error"
		path "org.zstack.network.service.vip.APIGetVipFreePortReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.7.11"
		clz ErrorCode.class
	}
}
