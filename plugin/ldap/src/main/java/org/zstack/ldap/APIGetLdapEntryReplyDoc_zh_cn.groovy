package org.zstack.ldap

import org.zstack.header.errorcode.ErrorCode

doc {

	title "Ldap条目列表"

	ref {
		name "error"
		path "org.zstack.ldap.APIGetLdapEntryReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	field {
		name "inventories"
		desc ""
		type "List"
		since "0.6"
	}
}
