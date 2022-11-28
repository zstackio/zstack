package org.zstack.header.vm.devices

import org.zstack.header.vm.devices.VmInstanceDeviceAddressArchiveInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询云主机设备地址归档的返回"

	ref {
		name "inventories"
		path "org.zstack.header.vm.devices.APIQueryVmInstanceDeviceAddressArchiveReply.inventories"
		desc "null"
		type "List"
		since "4.4.24"
		clz VmInstanceDeviceAddressArchiveInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.4.24"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.devices.APIQueryVmInstanceDeviceAddressArchiveReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.4.24"
		clz ErrorCode.class
	}
}
