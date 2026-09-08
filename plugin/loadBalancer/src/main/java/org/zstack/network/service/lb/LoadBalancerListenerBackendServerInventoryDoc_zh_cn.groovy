package org.zstack.network.service.lb

import java.lang.Long
import java.lang.Integer

doc {

	title "负载均衡监听器后端服务器清单"

	field {
		name "listenerUuid"
		desc "负载均衡监听器UUID"
		type "String"
		since "5.5.38"
	}
	field {
		name "serverGroupUuid"
		desc "后端服务器组UUID"
		type "String"
		since "5.5.38"
	}
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
		desc "IP类型后端服务器的配置地址"
		type "String"
		since "5.5.38"
	}
	field {
		name "ipAddress"
		desc "后端服务器的实际IP地址"
		type "String"
		since "5.5.38"
	}
	field {
		name "weight"
		desc "后端服务器权重"
		type "Long"
		since "5.5.38"
	}
	field {
		name "state"
		desc "后端服务器管理态"
		type "String"
		since "5.5.38"
	}
	field {
		name "runtimeStatus"
		desc "后端服务器运行态"
		type "String"
		since "5.5.38"
	}
	field {
		name "healthStatus"
		desc "后端服务器健康状态"
		type "String"
		since "5.5.38"
	}
	field {
		name "instancePort"
		desc "后端服务器端口"
		type "Integer"
		since "5.5.38"
	}
}
