package org.zstack.header.identity

doc {

    title "配额使用情况"

    field {
        name "name"
        desc "资源名称"
        type "String"
        since "0.6"
    }
    field {
        name "total"
        desc "配额总量"
        type "Long"
        since "0.6"
    }
    field {
        name "used"
        desc "配额已用量"
        type "Long"
        since "0.6"
    }
}
