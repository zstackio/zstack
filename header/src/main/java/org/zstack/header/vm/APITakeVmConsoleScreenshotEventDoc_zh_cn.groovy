package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取虚拟机控制台截图"

	field {
		name "success"
		desc ""
		type "boolean"
		since "4.7.0"
	}
	ref {
		name "error"
		path "org.zstack.header.console.APITakeVmConsoleScreenshotEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.7.0"
		clz ErrorCode.class
	}
	field {
		name "imageData"
		desc "控制台截图的base64"
		type "String"
		since "4.7.0"
	}
}
