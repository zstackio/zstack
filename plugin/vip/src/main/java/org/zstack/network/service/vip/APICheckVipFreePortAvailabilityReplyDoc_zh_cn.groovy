package org.zstack.network.service.vip

import org.zstack.header.errorcode.ErrorCode

doc {

	title "检查VIP端口是否空闲"

	field {
		name "available"
		desc ""
		type "boolean"
		since "4.7.11"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.7.11"
	}
	ref {
		name "error"
		path "org.zstack.network.service.vip.APICheckVipFreePortAvailabilityReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.7.11"
		clz ErrorCode.class
	}
}
