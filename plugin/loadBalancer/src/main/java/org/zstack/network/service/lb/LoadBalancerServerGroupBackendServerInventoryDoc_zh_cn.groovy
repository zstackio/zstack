package org.zstack.network.service.lb

import java.lang.Long
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "负载均衡服务器组后端服务器"

	field {
		name "uuid"
		desc "后端服务器唯一UUID,由服务器类型和记录ID确定性生成,跨请求稳定"
		type "String"
		since "5.5.38"
	}
	field {
		name "serverGroupName"
		desc "服务器组名称"
		type "String"
		since "5.5.38"
	}
	field {
		name "listenerUuid"
		desc "监听器UUID,state为该监听器视角的启停状态"
		type "String"
		since "5.5.38"
	}
	field {
		name "serverGroupUuid"
		desc "服务器组UUID"
		type "String"
		since "5.5.38"
	}
	field {
		name "loadBalancerUuid"
		desc "负载均衡器UUID"
		type "String"
		since "5.5.38"
	}
	field {
		name "serverType"
		desc "后端服务器类型,云主机网卡为VmInstance,外部IP为ServerIp"
		type "String"
		since "5.5.38"
	}
	field {
		name "vmNicUuid"
		desc "云主机网卡UUID,serverType为ServerIp时为null"
		type "String"
		since "5.5.38"
	}
	field {
		name "vmInstanceUuid"
		desc "云主机UUID,serverType为ServerIp时为null"
		type "String"
		since "5.5.38"
	}
	field {
		name "serverName"
		desc "服务器名称,云主机后端为云主机名称,外部IP后端为IP地址"
		type "String"
		since "5.5.38"
	}
	field {
		name "ip"
		desc "后端服务器IP地址"
		type "String"
		since "5.5.38"
	}
	field {
		name "l3NetworkUuid"
		desc "网卡所在三层网络UUID,serverType为ServerIp时为null"
		type "String"
		since "5.5.38"
	}
	field {
		name "weight"
		desc "权重"
		type "Long"
		since "5.5.38"
	}
	field {
		name "ipVersion"
		desc "IP协议版本,serverType为ServerIp时为null"
		type "Integer"
		since "5.5.38"
	}
	field {
		name "state"
		desc "启用状态,Enabled表示已启用,Disabled表示已禁用;不指定listenerUuid时为聚合结果"
		type "String"
		since "5.5.38"
	}
	field {
		name "runtimeStatus"
		desc "云主机运行联动状态,Active表示运行中,Inactive表示已停止,Pending表示等待中"
		type "String"
		since "5.5.38"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "5.5.38"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "5.5.38"
	}
}
