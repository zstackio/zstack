package org.zstack.header.vm.devices

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "云主机设备地址归档清单"

	field {
		name "id"
		desc ""
		type "long"
		since "4.4.24"
	}
	field {
		name "resourceUuid"
		desc "资源UUID"
		type "String"
		since "4.4.24"
	}
	field {
		name "vmInstanceUuid"
		desc "云主机UUID"
		type "String"
		since "4.4.24"
	}
	field {
		name "deviceAddress"
		desc ""
		type "String"
		since "4.4.24"
	}
	field {
		name "addressGroupUuid"
		desc ""
		type "String"
		since "4.4.24"
	}
	field {
		name "metadata"
		desc ""
		type "String"
		since "4.4.24"
	}
	field {
		name "metadataClass"
		desc ""
		type "String"
		since "4.4.24"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "4.4.24"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "4.4.24"
	}
}
