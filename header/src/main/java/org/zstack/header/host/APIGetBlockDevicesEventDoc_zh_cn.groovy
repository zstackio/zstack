package org.zstack.header.host

import org.zstack.header.host.HostBlockDeviceStruct
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取主机块设备事件"

	ref {
		name "blockDevices"
		path "org.zstack.header.host.APIGetBlockDevicesEvent.blockDevices"
		desc "主机上的块设备列表"
		type "List"
		since "5.5.28"
		clz HostBlockDeviceStruct.class
	}
	field {
		name "success"
		desc "操作是否成功"
		type "boolean"
		since "5.5.28"
	}
	ref {
		name "error"
		path "org.zstack.header.host.APIGetBlockDevicesEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.5.28"
		clz ErrorCode.class
	}
}
