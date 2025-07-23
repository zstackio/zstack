package org.zstack.network.hostNetworkInterface

import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

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
		desc ""
		type "String"
		since "5.3.28"
	}
	field {
		name "portType"
		desc ""
		type "String"
		since "5.3.28"
	}
	field {
		name "peerInterfaceUuid"
		desc ""
		type "String"
		since "5.3.28"
	}
	field {
		name "switchUuid"
		desc ""
		type "String"
		since "5.3.28"
	}
	field {
		name "sdnControllerUuid"
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
}
