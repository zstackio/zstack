package org.zstack.header.image

import org.zstack.header.image.ImageGroupInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询镜像组的返回"

	ref {
		name "inventories"
		path "org.zstack.header.image.APIQueryImageGroupReply.inventories"
		desc "null"
		type "List"
		since "5.3.36"
		clz ImageGroupInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.3.36"
	}
	ref {
		name "error"
		path "org.zstack.header.image.APIQueryImageGroupReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.3.36"
		clz ErrorCode.class
	}
}
