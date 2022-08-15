package org.zstack.sdnController.header

doc {

	title "SDN控制器清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.7"
	}
	field {
		name "vendorType"
		desc ""
		type "String"
		since "3.7"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.7"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "3.7"
	}
	field {
		name "ip"
		desc ""
		type "String"
		since "3.7"
	}
	field {
		name "username"
		desc ""
		type "String"
		since "3.7"
	}
	field {
		name "password"
		desc ""
		type "String"
		since "3.7"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.7"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.7"
	}
	ref {
		name "vniRanges"
		path "org.zstack.sdnController.header.SdnControllerInventory.vniRanges"
		desc "null"
		type "List"
		since "3.7"
		clz SdnVniRange.class
	}
	ref {
		name "vxlanPools"
		path "org.zstack.sdnController.header.SdnControllerInventory.vxlanPools"
		desc "null"
		type "List"
		since "3.7"
		clz HardwareL2VxlanNetworkPoolInventory.class
	}
}
