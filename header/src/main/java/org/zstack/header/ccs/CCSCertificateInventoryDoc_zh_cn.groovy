package org.zstack.header.ccs

doc {

	title "在这里输入结构的名称"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "0.6"
	}
	field {
		name "algorithm"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "format"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "issuerDN"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "subjectDN"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "serNumber"
		desc ""
		type "Long"
		since "0.6"
	}
	field {
		name "effectiveTime"
		desc ""
		type "Timestamp"
		since "0.6"
	}
	field {
		name "expirationTime"
		desc ""
		type "Timestamp"
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
	ref {
		name "userCertificateRefs"
		path "org.zstack.header.ccs.CCSCertificateInventory.userCertificateRefs"
		desc "null"
		type "List"
		since "0.6"
		clz CCSCertificateUserRefInventory.class
	}
}
