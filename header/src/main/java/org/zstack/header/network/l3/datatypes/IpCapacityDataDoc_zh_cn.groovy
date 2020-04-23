package org.zstack.header.network.l3.datatypes



doc {

	title "资源的IP地址容量信息结构"

	field {
		name "resourceUuid"
		desc "资源UUID"
		type "String"
		since "3.9.0"
	}
	field {
		name "totalCapacity"
		desc "IP地址总容量"
		type "long"
		since "3.9.0"
	}
	field {
		name "availableCapacity"
		desc "可用IP地址容量"
		type "long"
		since "3.9.0"
	}
	field {
		name "usedIpAddressNumber"
		desc "已用IP地址容量"
		type "long"
		since "3.9.0"
	}
}
