package org.zstack.header.vm.devices

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "虚拟机设备地址归档清单"

	field {
		name "id"
		desc ""
		type "long"
		since "3.14.24"
	}
	field {
		name "resourceUuid"
		desc "资源 UUID"
		type "String"
		since "3.14.24"
	}
	field {
		name "vmInstanceUuid"
		desc "虚拟机 UUID"
		type "String"
		since "3.14.24"
	}
	field {
		name "deviceAddress"
		desc "设备地址"
		type "String"
		since "3.14.24"
	}
	field {
		name "addressGroupUuid"
		desc ""
		type "String"
		since "3.14.24"
	}
	field {
		name "metadata"
		desc ""
		type "String"
		since "3.14.24"
	}
	field {
		name "metadataClass"
		desc ""
		type "String"
		since "3.14.24"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.14.24"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.14.24"
	}
}
