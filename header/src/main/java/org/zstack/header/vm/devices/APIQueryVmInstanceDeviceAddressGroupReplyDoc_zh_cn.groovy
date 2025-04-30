package org.zstack.header.vm.devices

import org.zstack.header.vm.devices.VmInstanceDeviceAddressGroupInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询虚拟机设备地址组的返回"

	ref {
		name "inventories"
		path "org.zstack.header.vm.devices.APIQueryVmInstanceDeviceAddressGroupReply.inventories"
		desc "虚拟机设备地址组清单列表"
		type "List"
		since "3.14.24"
		clz VmInstanceDeviceAddressGroupInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.14.24"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.devices.APIQueryVmInstanceDeviceAddressGroupReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.14.24"
		clz ErrorCode.class
	}
}
