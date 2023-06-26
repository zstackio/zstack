package org.zstack.header.vm.devices

import java.sql.Timestamp
import java.sql.Timestamp
import org.zstack.header.vm.devices.VmInstanceDeviceAddressArchiveInventory

doc {

	title "在这里输入结构的名称"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "0.6"
	}
	field {
		name "resourceUuid"
		desc "资源UUID"
		type "String"
		since "0.6"
	}
	field {
		name "vmInstanceUuid"
		desc "云主机UUID"
		type "String"
		since "0.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "0.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "0.6"
	}
	ref {
		name "addressList"
		path "org.zstack.header.vm.devices.VmInstanceDeviceAddressGroupInventory.addressList"
		desc "null"
		type "List"
		since "0.6"
		clz VmInstanceDeviceAddressArchiveInventory.class
	}
}
