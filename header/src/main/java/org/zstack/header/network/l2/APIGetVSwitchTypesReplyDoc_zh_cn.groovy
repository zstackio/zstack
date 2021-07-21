package org.zstack.header.network.l2

import org.zstack.header.errorcode.ErrorCode

doc {

    title "虚拟交换机清单"

    ref {
        name "error"
        path "org.zstack.header.network.l2.APIGetVSwitchTypesReply.error"
        desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
        type "ErrorCode"
        since "4.1.0"
        clz ErrorCode.class
    }
	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
    field {
        name "types"
        desc ""
        type "List"
        since "4.1.0"
    }
}