package org.zstack.header.vm.devices

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

	field {
		name "id"
		desc ""
		type "long"
		since "0.6"
	}
	field {
		name "resourceUuid"
		desc "资源UUID"
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
		name "deviceAddress"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "addressGroupUuid"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "metadata"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "metadataClass"
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
