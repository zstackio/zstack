package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode

doc {

	title "回收云主机磁盘空间返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "4.7.21"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIVmFstrimEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.7.21"
		clz ErrorCode.class
	}
}
