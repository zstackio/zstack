package org.zstack.network.l2.vxlan.vxlanNetworkPool

import java.lang.Integer
import java.lang.Integer

doc {

	title "在这里输入结构的名称"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "0.6"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "0.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "0.6"
	}
	field {
		name "startVni"
		desc ""
		type "Integer"
		since "0.6"
	}
	field {
		name "endVni"
		desc ""
		type "Integer"
		since "0.6"
	}
	field {
		name "l2NetworkUuid"
		desc "二层网络UUID"
		type "String"
		since "0.6"
	}
}
