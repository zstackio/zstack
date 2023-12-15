package org.zstack.header.image

import org.zstack.header.image.ImageInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取镜像MD5值返回"

	ref {
		name "inventory"
		path "org.zstack.header.image.APICalculateImageHashEvent.inventory"
		desc "null"
		type "ImageInventory"
		since "4.1.0"
		clz ImageInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.1.0"
	}
	ref {
		name "error"
		path "org.zstack.header.image.APICalculateImageHashEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.1.0"
		clz ErrorCode.class
	}
}
