package org.zstack.network.service.lb



doc {

	title "负载均衡后端服务器状态变更结果清单"

	field {
		name "backendType"
		desc "后端服务器类型，取值为VmNic或ServerIp"
		type "String"
		since "5.5.38"
	}
	field {
		name "vmNicUuid"
		desc "云主机网卡UUID"
		type "String"
		since "5.5.38"
	}
	field {
		name "serverIp"
		desc "后端服务器IP地址"
		type "String"
		since "5.5.38"
	}
	field {
		name "targetState"
		desc "请求设置的目标管理态"
		type "String"
		since "5.5.38"
	}
	field {
		name "effectiveState"
		desc "实际生效的管理态"
		type "String"
		since "5.5.38"
	}
}
