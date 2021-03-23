package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode

doc {

	title "网卡已加载的网络服务名称"

	ref {
		name "error"
		path "org.zstack.header.vm.APIGetVmNicAttachedNetworkServiceReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.1.0"
		clz ErrorCode.class
	}
	field {
		name "networkServices"
		desc ""
		type "List"
		since "4.1.0"
	}
}
