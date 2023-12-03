package org.zstack.resourceconfig

import org.zstack.header.errorcode.ErrorCode
import org.zstack.resourceconfig.ResourceConfigStruct

doc {

	title "查询多个资源级配置的返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "4.7.0"
	}
	ref {
		name "error"
		path "org.zstack.resourceconfig.APIGetResourceConfigsReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.7.0"
		clz ErrorCode.class
	}
	ref {
		name "configs"
		path "org.zstack.resourceconfig.APIGetResourceConfigsReply.configs"
		desc "null"
		type "List"
		since "4.7.0"
		clz ResourceConfigStruct.class
	}
}
