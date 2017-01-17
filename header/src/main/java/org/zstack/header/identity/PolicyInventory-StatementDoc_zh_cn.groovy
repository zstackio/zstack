package org.zstack.header.identity

import org.zstack.header.identity.AccountConstant.StatementEffect

doc {

    title "策略声明清单"

    field {
        name "name"
        desc "资源名称"
        type "String"
        since "0.6"
    }
    ref {
        name "effect"
        path "org.zstack.header.identity.PolicyInventory.Statement.effect"
        desc "声明的效果（许可，禁止）"
        type "StatementEffect"
        since "0.6"
        clz StatementEffect.class
    }
    field {
        name "principals"
        desc ""
        type "List"
        since "0.6"
    }
    field {
        name "actions"
        desc "一个匹配API的字符串列表"
        type "List"
        since "0.6"
    }
    field {
        name "resources"
        desc "资源列表"
        type "List"
        since "0.6"
    }
}
