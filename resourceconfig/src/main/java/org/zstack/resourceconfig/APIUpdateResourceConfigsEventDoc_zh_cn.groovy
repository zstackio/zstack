package org.zstack.resourceconfig

import org.zstack.resourceconfig.ResourceConfigStruct
import org.zstack.header.errorcode.ErrorCode

doc {

	title "资源高级设置清单"

	ref {
		name "inventories"
		path "org.zstack.resourceconfig.APIUpdateResourceConfigsEvent.inventories"
		desc "null"
		type "List"
		since "4.7.0"
		clz ResourceConfigStruct.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.7.0"
	}
	ref {
		name "error"
		path "org.zstack.resourceconfig.APIUpdateResourceConfigsEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.7.0"
		clz ErrorCode.class
	}
}
