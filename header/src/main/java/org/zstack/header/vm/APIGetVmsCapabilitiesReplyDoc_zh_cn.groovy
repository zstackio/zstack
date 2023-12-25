package org.zstack.header.vm

import org.zstack.header.vm.VmCapabilities
import org.zstack.header.errorcode.ErrorCode

doc {

	title "批量获取云主机能力返回"

	ref {
		name "vmsCaps"
		path "org.zstack.header.vm.APIGetVmsCapabilitiesReply.vmsCaps"
		desc "null"
		type "Map"
		since "4.0"
		clz VmCapabilities.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.0"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIGetVmsCapabilitiesReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.0"
		clz ErrorCode.class
	}
}
