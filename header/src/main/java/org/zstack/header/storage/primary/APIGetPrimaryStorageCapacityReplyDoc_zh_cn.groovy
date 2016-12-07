package org.zstack.header.storage.primary

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.ErrorCode

doc {

	title "在这里输入结构的名称"

	ref {
		name "error"
		path "org.zstack.header.storage.primary.APIGetPrimaryStorageCapacityReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	field {
		name "totalCapacity"
		desc ""
		type "long"
		since "0.6"
	}
	field {
		name "availableCapacity"
		desc ""
		type "long"
		since "0.6"
	}
	field {
		name "totalPhysicalCapacity"
		desc ""
		type "long"
		since "0.6"
	}
	field {
		name "availablePhysicalCapacity"
		desc ""
		type "long"
		since "0.6"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.primary.APIGetPrimaryStorageCapacityReply.error"
		desc "null"
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
}
