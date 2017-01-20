package org.zstack.header.image

import org.zstack.header.errorcode.ErrorCode

doc {

	title "彻底删除镜像"

	ref {
		name "error"
		path "org.zstack.header.image.APIExpungeImageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
}
