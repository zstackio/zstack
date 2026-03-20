package org.zstack.header.host

import org.zstack.header.host.HostBlockDeviceStruct
import org.zstack.header.errorcode.ErrorCode

doc {

	title "在这里输入结构的名称"

	ref {
		name "blockDevices"
		path "org.zstack.header.host.APIGetBlockDevicesEvent.blockDevices"
		desc "null"
		type "List"
		since "5.5.6"
		clz HostBlockDeviceStruct.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.5.6"
	}
	ref {
		name "error"
		path "org.zstack.header.host.APIGetBlockDevicesEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.5.6"
		clz ErrorCode.class
	}
}
