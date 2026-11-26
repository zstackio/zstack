package org.zstack.kvm.memory

import org.zstack.header.errorcode.ErrorCode

doc {
    title "物理机交换盘查询响应"

    field {
        name "success"
        desc "API调用是否成功。"
        type "boolean"
        since "5.7.10"
    }
    ref {
        name "error"
        path "org.zstack.kvm.memory.APIGetHostMemorySwapDisksReply.error"
        desc "错误码，若不为null，则表示操作失败，操作成功时该字段为null。", false
        type "ErrorCode"
        since "5.7.10"
        clz ErrorCode.class
    }
    ref {
        name "inventory"
        path "org.zstack.kvm.memory.APIGetHostMemorySwapDisksReply.inventory"
        desc "物理机交换盘及候选盘，容量单位为字节。"
        type "MemorySwapDiskInventory"
        since "5.7.10"
        clz MemorySwapDiskInventory.class
    }
}
