package org.zstack.header.identity

doc {

    title "共享资源清单"

    field {
        name "ownerAccountUuid"
        desc "所有者账户UUID"
        type "String"
        since "0.6"
    }
    field {
        name "receiverAccountUuid"
        desc "接受者账户UUID"
        type "String"
        since "0.6"
    }
    field {
        name "toPublic"
        desc "是否全局共享"
        type "Boolean"
        since "0.6"
    }
    field {
        name "resourceType"
        desc "资源类型"
        type "String"
        since "0.6"
    }
    field {
        name "resourceUuid"
        desc "资源UUID"
        type "String"
        since "0.6"
    }
    field {
        name "lastOpDate"
        desc "最后一次修改时间"
        type "Timestamp"
        since "0.6"
    }
    field {
        name "createDate"
        desc "创建时间"
        type "Timestamp"
        since "0.6"
    }
}
