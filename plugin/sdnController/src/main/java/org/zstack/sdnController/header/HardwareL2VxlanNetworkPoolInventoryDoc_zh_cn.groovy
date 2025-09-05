package org.zstack.sdnController.header

doc {

	title "在这里输入结构的名称"

	field {
		name "sdnControllerUuid"
		desc ""
		type "String"
		since "5.3.28"
	}
	ref {
		name "attachedVtepRefs"
		path "org.zstack.sdnController.header.HardwareL2VxlanNetworkPoolInventory.attachedVtepRefs"
		desc "null"
		type "List"
		since "5.3.28"
		clz VtepInventory.class
	}
	ref {
		name "remoteVteps"
		path "org.zstack.sdnController.header.HardwareL2VxlanNetworkPoolInventory.remoteVteps"
		desc "null"
		type "List"
		since "5.3.28"
		clz RemoteVtepInventory.class
	}
	ref {
		name "attachedVxlanNetworkRefs"
		path "org.zstack.sdnController.header.HardwareL2VxlanNetworkPoolInventory.attachedVxlanNetworkRefs"
		desc "null"
		type "List"
		since "5.3.28"
		clz L2VxlanNetworkInventory.class
	}
	ref {
		name "attachedVniRanges"
		path "org.zstack.sdnController.header.HardwareL2VxlanNetworkPoolInventory.attachedVniRanges"
		desc "null"
		type "List"
		since "5.3.28"
		clz VniRangeInventory.class
	}
	field {
		name "attachedCidrs"
		desc ""
		type "Map"
		since "5.3.28"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "5.3.28"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "5.3.28"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "5.3.28"
	}
	field {
		name "zoneUuid"
		desc "区域UUID"
		type "String"
		since "5.3.28"
	}
	field {
		name "physicalInterface"
		desc ""
		type "String"
		since "5.3.28"
	}
	field {
		name "type"
		desc ""
		type "String"
		since "5.3.28"
	}
	field {
		name "vSwitchType"
		desc ""
		type "String"
		since "5.3.28"
	}
	field {
		name "virtualNetworkId"
		desc ""
		type "Integer"
		since "5.3.28"
	}
	field {
		name "isolated"
		desc ""
		type "Boolean"
		since "5.3.28"
	}
	field {
		name "pvlan"
		desc ""
		type "String"
		since "5.3.28"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "5.3.28"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "5.3.28"
	}
	field {
		name "attachedClusterUuids"
		desc ""
		type "List"
		since "5.3.28"
	}
}
