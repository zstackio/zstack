package org.zstack.header.identity

import org.zstack.header.identity.PolicyInventory.Statement

doc {

    title "策略清单"

    ref {
        name "statements"
        path "org.zstack.header.identity.PolicyInventory.statements"
        desc "声明清单列表"
        type "List"
        since "0.6"
        clz Statement.class
    }
    field {
        name "name"
        desc "资源名称"
        type "String"
        since "0.6"
    }
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
}
