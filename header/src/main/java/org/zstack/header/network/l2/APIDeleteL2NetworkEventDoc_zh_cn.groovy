package org.zstack.header.network.l2

import org.zstack.header.errorcode.ErrorCode

doc {

	title "二层网络清单"

	ref {
		name "error"
		path "org.zstack.header.network.l2.APIDeleteL2NetworkEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
}
