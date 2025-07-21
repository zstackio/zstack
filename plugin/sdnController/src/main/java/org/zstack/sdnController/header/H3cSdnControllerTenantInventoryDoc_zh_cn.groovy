package org.zstack.sdnController.header

import java.sql.Timestamp

doc {

	title "SDN控制器租户清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
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
		name "tenantUuid"
		desc ""
		type "String"
		since "5.3.28"
	}
	field {
		name "vdsUuid"
		desc ""
		type "String"
		since "5.3.28"
	}
	field {
		name "tenantName"
		desc ""
		type "String"
		since "5.3.28"
	}
	field {
		name "vdsName"
		desc ""
		type "String"
		since "5.3.28"
	}
	field {
		name "cloudDomainName"
		desc ""
		type "String"
		since "5.3.28"
	}
	field {
		name "status"
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
