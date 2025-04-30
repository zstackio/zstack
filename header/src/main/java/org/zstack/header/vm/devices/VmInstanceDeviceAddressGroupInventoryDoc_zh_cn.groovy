package org.zstack.header.vm.devices

import java.sql.Timestamp
import java.sql.Timestamp
import org.zstack.header.vm.devices.VmInstanceDeviceAddressArchiveInventory

doc {

	title "虚拟机设备地址组清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.14.24"
	}
	field {
		name "resourceUuid"
		desc "资源 UUID"
		type "String"
		since "3.14.24"
	}
	field {
		name "vmInstanceUuid"
		desc "虚拟机 UUID"
		type "String"
		since "3.14.24"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.14.24"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.14.24"
	}
	ref {
		name "addressList"
		path "org.zstack.header.vm.devices.VmInstanceDeviceAddressGroupInventory.addressList"
		desc "虚拟机地址清单列表"
		type "List"
		since "3.14.24"
		clz VmInstanceDeviceAddressArchiveInventory.class
	}
}
