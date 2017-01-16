package org.zstack.header.identity

doc {

    title "用户组清单"

    field {
        name "uuid"
        desc "资源的UUID，唯一标示该资源"
        type "String"
        since "0.6"
    }
    field {
        name "accountUuid"
        desc "账户UUID"
        type "String"
        since "0.6"
    }
    field {
        name "name"
        desc "资源名称"
        type "String"
        since "0.6"
    }
    field {
        name "description"
        desc "资源的详细描述"
        type "String"
        since "0.6"
    }
    field {
        name "createDate"
        desc "创建时间"
        type "Timestamp"
        since "0.6"
    }
    field {
        name "lastOpDate"
        desc "最后一次修改时间"
        type "Timestamp"
        since "0.6"
    }
}
