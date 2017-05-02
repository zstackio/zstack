package org.zstack.header.vo



doc {

	title "资源UUID、名称结构"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "2.0"
	}
	field {
		name "resourceName"
		desc "资源名称"
		type "String"
		since "2.0"
	}
	field {
		name "resourceType"
		desc "资源类型，例如虚拟机为VmInstanceVO"
		type "String"
		since "2.0"
	}
}
