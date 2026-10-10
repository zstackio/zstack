package org.zstack.kvm.memory

import org.zstack.header.errorcode.ErrorCode

doc {
    title "Host写回后端列表响应"

    field {
        name "success"
        desc "API调用是否成功。"
        type "boolean"
        since "5.5.38"
    }
    ref {
        name "error"
        path "org.zstack.kvm.memory.APIGetHostMemoryWritebackBackendsReply.error"
        desc "错误码，若不为null，则表示操作失败，操作成功时该字段为null。", false
        type "ErrorCode"
        since "5.5.38"
        clz ErrorCode.class
    }
    ref {
        name "inventory"
        path "org.zstack.kvm.memory.APIGetHostMemoryWritebackBackendsReply.inventory"
        desc "Host后端候选清单；只包含已定义的公开字段，容量单位为bytes。"
        type "MemoryWritebackBackendInventory"
        since "5.5.38"
        clz MemoryWritebackBackendInventory.class
    }
}
