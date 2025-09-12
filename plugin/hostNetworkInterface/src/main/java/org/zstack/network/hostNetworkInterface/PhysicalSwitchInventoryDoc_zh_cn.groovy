package org.zstack.network.hostNetworkInterface

import java.sql.Timestamp
import org.zstack.network.hostNetworkInterface.PhysicalSwitchPortInventory

doc {

	title "物理交换机清单"

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
		name "ip"
		desc "物理交换机管理IP地址"
		type "String"
		since "5.3.28"
	}
	field {
		name "mac"
		desc "物理交换机MAC地址"
		type "String"
		since "5.3.28"
	}
	field {
		name "mode"
		desc "物理交换机工作模式(Switch,Router)"
		type "String"
		since "5.3.28"
	}
	field {
		name "softwareVersion"
		desc "物理交换机软件版本"
		type "String"
		since "5.3.28"
	}
	field {
		name "sdnControllerUuid"
		desc "SDN控制器uuid"
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
	ref {
		name "ports"
		path "org.zstack.network.hostNetworkInterface.PhysicalSwitchInventory.ports"
		desc "null"
		type "List"
		since "5.3.28"
		clz PhysicalSwitchPortInventory.class
	}
}
