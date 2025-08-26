package org.zstack.header.image

import org.zstack.header.image.ImageInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "克隆镜像的执行结果"

	ref {
		name "inventory"
		path "org.zstack.header.image.APICloneImageEvent.inventory"
		desc "null"
		type "ImageInventory"
		since "5.4.0"
		clz ImageInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.4.0"
	}
	ref {
		name "error"
		path "org.zstack.header.image.APICloneImageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.4.0"
		clz ErrorCode.class
	}
}
