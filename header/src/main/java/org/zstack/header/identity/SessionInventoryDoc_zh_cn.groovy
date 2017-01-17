package org.zstack.header.identity

doc {

    title "会话清单"

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
        name "userUuid"
        desc "用户UUID"
        type "String"
        since "0.6"
    }
    field {
        name "expiredDate"
        desc "会话过期日期"
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
