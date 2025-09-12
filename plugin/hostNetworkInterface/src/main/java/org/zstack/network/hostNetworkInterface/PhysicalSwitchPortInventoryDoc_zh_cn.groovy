package org.zstack.network.hostNetworkInterface

import java.sql.Timestamp

doc {

	title "物理交换机端口清单"

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
		name "ethTrunkName"
		desc "物理交换机端口所属的trunk名称"
		type "String"
		since "5.3.28"
	}
	field {
		name "portType"
		desc "物理交换机端口链路类型"
		type "String"
		since "5.3.28"
	}
	field {
		name "peerInterfaceUuid"
		desc "物理交换机端口连接服务器接口uuid"
		type "String"
		since "5.3.28"
	}
	field {
		name "switchUuid"
		desc "物理交换机端口所属交换机uuid"
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
}
