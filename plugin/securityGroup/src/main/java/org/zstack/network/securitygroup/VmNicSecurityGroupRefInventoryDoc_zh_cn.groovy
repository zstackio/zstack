package org.zstack.network.securitygroup

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "安全组下的网卡清单"

	field {
		name "vmNicUuid"
		desc "云主机网卡UUID"
		type "String"
		since "0.6"
	}
	field {
		name "securityGroupUuid"
		desc "安全组UUID"
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
}
