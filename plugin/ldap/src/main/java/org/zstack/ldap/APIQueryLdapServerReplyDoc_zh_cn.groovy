package org.zstack.ldap

import org.zstack.header.errorcode.ErrorCode

doc {

    title "LDAP服务器清单列表"

    ref {
        name "error"
        path "org.zstack.ldap.APIQueryLdapServerReply.error"
        desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
        type "ErrorCode"
        since "0.6"
        clz ErrorCode.class
    }
    ref {
        name "inventories"
        path "org.zstack.ldap.APIQueryLdapServerReply.inventories"
        desc "LDAP服务器清单列表"
        type "List"
        since "0.6"
        clz LdapServerInventory.class
    }
}
