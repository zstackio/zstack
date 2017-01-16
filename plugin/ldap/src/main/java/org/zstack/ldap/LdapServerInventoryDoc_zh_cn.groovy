package org.zstack.ldap

doc {

    title "LDAP服务器清单"

    field {
        name "uuid"
        desc "资源的UUID，唯一标示该资源"
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
        name "url"
        desc ""
        type "String"
        since "0.6"
    }
    field {
        name "base"
        desc "LDAP服务器访问地址"
        type "String"
        since "0.6"
    }
    field {
        name "username"
        desc "用于访问LDAP服务器的用户名"
        type "String"
        since "0.6"
    }
    field {
        name "password"
        desc "密码"
        type "String"
        since "0.6"
    }
    field {
        name "encryption"
        desc "加密方式"
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
