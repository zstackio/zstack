package org.zstack.header.vm.cdrom

import org.zstack.header.errorcode.ErrorCode

doc {

	title "删除CDROM返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.cdrom.APIDeleteVmCdRomEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.3"
		clz ErrorCode.class
	}
}
