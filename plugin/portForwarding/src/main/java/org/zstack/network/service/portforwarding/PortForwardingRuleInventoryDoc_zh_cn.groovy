package org.zstack.network.service.portforwarding

import java.lang.Integer
import java.lang.Integer
import java.lang.Integer
import java.lang.Integer
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "0.6"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "0.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "0.6"
	}
	field {
		name "vipIp"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "guestIp"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "vipUuid"
		desc "VIP UUID"
		type "String"
		since "0.6"
	}
	field {
		name "vipPortStart"
		desc ""
		type "Integer"
		since "0.6"
	}
	field {
		name "vipPortEnd"
		desc ""
		type "Integer"
		since "0.6"
	}
	field {
		name "privatePortStart"
		desc ""
		type "Integer"
		since "0.6"
	}
	field {
		name "privatePortEnd"
		desc ""
		type "Integer"
		since "0.6"
	}
	field {
		name "vmNicUuid"
		desc "云主机网卡UUID"
		type "String"
		since "0.6"
	}
	field {
		name "protocolType"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "state"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "allowedCidr"
		desc ""
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
}
