package org.zstack.header.core.trash

import org.zstack.header.errorcode.ErrorCode
import java.lang.Long

doc {

    title "清理回收站数据返回信息"

    field {
        name "resourceUuid"
        desc "清理数据对应的UUID"
        type "String"
        since "4.7.0"
    }
    field {
        name "success"
        desc ""
        type "boolean"
        since "4.7.0"
    }
    ref {
        name "error"
        path "org.zstack.header.core.trash.TrashCleanupResult.error"
        desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
        type "ErrorCode"
        since "4.7.0"
        clz ErrorCode.class
    }
    field {
        name "trashId"
        desc "清理数据对应的ID"
        type "long"
        since "4.7.0"
    }
    field {
        name "size"
        desc "清理数据对应的大小"
        type "Long"
        since "4.7.0"
    }
}
