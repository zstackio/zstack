package org.zstack.header.core.external.service

import org.zstack.header.errorcode.ErrorCode

doc {

	title "重新加载External Services的返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "4.7.0"
	}
	ref {
		name "error"
		path "org.zstack.header.core.external.service.APIReloadExternalServiceEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.7.0"
		clz ErrorCode.class
	}
}
