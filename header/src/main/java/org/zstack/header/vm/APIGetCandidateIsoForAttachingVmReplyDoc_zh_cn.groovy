package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.image.ImageInventory

doc {

    title "ISO清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
    ref {
        name "error"
        path "org.zstack.header.vm.APIGetCandidateIsoForAttachingVmReply.error"
        desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
        type "ErrorCode"
        since "0.6"
        clz ErrorCode.class
    }
    ref {
        name "inventories"
        path "org.zstack.header.vm.APIGetCandidateIsoForAttachingVmReply.inventories"
        desc "ISO清单"
        type "List"
        since "0.6"
        clz ImageInventory.class
    }
}
