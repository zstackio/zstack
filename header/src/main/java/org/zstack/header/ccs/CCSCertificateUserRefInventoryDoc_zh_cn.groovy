package org.zstack.header.ccs

doc {

	title "在这里输入结构的名称"

	field {
		name "userUuid"
		desc "用户UUID"
		type "String"
		since "0.6"
	}
	field {
		name "certificateUuid"
		desc ""
		type "String"
		since "0.6"
	}
	ref {
		name "state"
		path "org.zstack.header.ccs.CCSCertificateUserRefInventory.state"
		desc "null"
		type "CCSCertificateUserState"
		since "0.6"
		clz CCSCertificateUserState.class
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
