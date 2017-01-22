package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode

doc {

    title "云主机清单"

    ref {
        name "error"
        path "org.zstack.header.vm.APIGetCandidateVmForAttachingIsoReply.error"
        desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
        type "ErrorCode"
        since "0.6"
        clz ErrorCode.class
    }
    ref {
        name "inventories"
        path "org.zstack.header.vm.APIGetCandidateVmForAttachingIsoReply.inventories"
        desc "云主机清单"
        type "List"
        since "0.6"
        clz VmInstanceInventory.class
    }
}
