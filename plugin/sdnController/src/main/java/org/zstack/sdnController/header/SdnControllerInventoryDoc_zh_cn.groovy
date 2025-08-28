package org.zstack.sdnController.header

import org.zstack.sdnController.header.SdnControllerStatus
import java.sql.Timestamp
import org.zstack.sdnController.header.SdnVniRange
import org.zstack.sdnController.header.HardwareL2VxlanNetworkPoolInventory
import org.zstack.sdnController.header.SdnControllerHostRefInventory

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
		name "vendorVersion"
		desc "厂商版本"
		type "String"
		since "5.4"
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
	ref {
		name "status"
		path "org.zstack.sdnController.header.SdnControllerInventory.status"
		desc "null"
		type "SdnControllerStatus"
		since "5.3.0"
		clz SdnControllerStatus.class
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
	ref {
		name "hostRefs"
		path "org.zstack.sdnController.header.SdnControllerInventory.hostRefs"
		desc "null"
		type "List"
		since "5.3.0"
		clz SdnControllerHostRefInventory.class
	}
}
