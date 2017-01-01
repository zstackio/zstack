package org.zstack.ldap

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.identity.SessionInventory
import org.zstack.header.identity.AccountInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "在这里输入结构的名称"

	ref {
		name "error"
		path "org.zstack.ldap.APILogInByLdapReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.ldap.APILogInByLdapReply.inventory"
		desc "null"
		type "SessionInventory"
		since "0.6"
		clz SessionInventory.class
	}
	ref {
		name "accountInventory"
		path "org.zstack.ldap.APILogInByLdapReply.accountInventory"
		desc "null"
		type "AccountInventory"
		since "0.6"
		clz AccountInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.ldap.APILogInByLdapReply.error"
		desc "null"
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
}
