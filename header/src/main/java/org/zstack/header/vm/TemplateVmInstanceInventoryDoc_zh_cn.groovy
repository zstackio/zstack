package org.zstack.header.vm



doc {

	title "虚拟机模板"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "zsv 4.2.6"
	}
	field {
		name "name"
		desc "模板名称"
		type "String"
		since "zsv 4.2.6"
	}
	field {
		name "zoneUuid"
		desc "数据中心UUID"
		type "String"
		since "zsv 4.2.6"
	}
}
