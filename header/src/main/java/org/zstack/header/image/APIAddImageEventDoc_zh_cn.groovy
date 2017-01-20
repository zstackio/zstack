package org.zstack.header.image

import org.zstack.header.errorcode.ErrorCode

doc {

	title "镜像清单"

	ref {
		name "error"
		path "org.zstack.header.image.APIAddImageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.image.APIAddImageEvent.inventory"
		desc "null"
		type "ImageInventory"
		since "0.6"
		clz ImageInventory.class
	}
}
