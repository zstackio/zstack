package org.zstack.header.image

import org.zstack.header.errorcode.ErrorCode

doc {

	title "删除镜像组的返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "5.4.0"
	}
	ref {
		name "error"
		path "org.zstack.header.image.APIExpungeImageGroupEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "5.4.0"
		clz ErrorCode.class
	}
}
