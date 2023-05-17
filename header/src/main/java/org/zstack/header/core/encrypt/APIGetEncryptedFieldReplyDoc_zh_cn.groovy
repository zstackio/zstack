package org.zstack.header.core.encrypt

import org.zstack.header.errorcode.ErrorCode

doc {

	title "加密字段返回"

	field {
		name "encryptedFields"
		desc "所有加密字段列表，列表中每个项是jsonString格式"
		type "List"
		since "4.7.0"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.7.0"
	}
	ref {
		name "error"
		path "org.zstack.header.core.encrypt.APIGetEncryptedFieldReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.7.0"
		clz ErrorCode.class
	}
}
