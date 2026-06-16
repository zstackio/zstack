package org.zstack.header.storage.addon.primary

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageHostProtocolRefInventory

doc {

    title "查询外接主存储按协议的主机连通性结果"

    field {
        name "success"
        desc ""
        type "boolean"
        since "0.6"
    }
    ref {
        name "error"
        path "org.zstack.header.storage.addon.primary.APIQueryExternalPrimaryStorageHostProtocolRefReply.error"
        desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
        type "ErrorCode"
        since "2.3.2"
        clz ErrorCode.class
    }
    ref {
        name "inventories"
        path "org.zstack.header.storage.addon.primary.APIQueryExternalPrimaryStorageHostProtocolRefReply.inventories"
        desc "null"
        type "List"
        since "2.3.2"
        clz ExternalPrimaryStorageHostProtocolRefInventory.class
    }
}
