package org.zstack.sdnController.header

import org.zstack.network.l2.vxlan.vtep.VtepInventory
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VniRangeInventory
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "硬件VXLAN资源池器清单"

	field {
		name "sdnControllerUuid"
		desc ""
		type "String"
		since "3.7"
	}
	ref {
		name "attachedVtepRefs"
		path "org.zstack.sdnController.header.HardwareL2VxlanNetworkPoolInventory.attachedVtepRefs"
		desc "null"
		type "List"
		since "3.7"
		clz VtepInventory.class
	}
	ref {
		name "attachedVxlanNetworkRefs"
		path "org.zstack.sdnController.header.HardwareL2VxlanNetworkPoolInventory.attachedVxlanNetworkRefs"
		desc "null"
		type "List"
		since "3.7"
		clz L2VxlanNetworkInventory.class
	}
	ref {
		name "attachedVniRanges"
		path "org.zstack.sdnController.header.HardwareL2VxlanNetworkPoolInventory.attachedVniRanges"
		desc "null"
		type "List"
		since "3.7"
		clz VniRangeInventory.class
	}
	field {
		name "attachedCidrs"
		desc ""
		type "Map"
		since "3.7"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
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
		name "zoneUuid"
		desc "区域UUID"
		type "String"
		since "3.7"
	}
	field {
		name "physicalInterface"
		desc ""
		type "String"
		since "3.7"
	}
	field {
		name "type"
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
	field {
		name "attachedClusterUuids"
		desc ""
		type "List"
		since "3.7"
	}
}
