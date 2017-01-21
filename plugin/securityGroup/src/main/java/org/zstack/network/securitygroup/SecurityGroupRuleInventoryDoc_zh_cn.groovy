package org.zstack.network.securitygroup

import java.lang.Integer
import java.lang.Integer
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "安全组规则清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "0.6"
	}
	field {
		name "securityGroupUuid"
		desc "安全组UUID"
		type "String"
		since "0.6"
	}
	field {
		name "type"
		desc "流量类型"
		type "String"
		since "0.6"
	}
	field {
		name "startPort"
		desc "如果协议是TCP/UDP, 它是端口范围（port range）的起始端口号; 如果协议是ICMP, 它是ICMP类型（type）"
		type "Integer"
		since "0.6"
	}
	field {
		name "endPort"
		desc "如果协议是TCP/UDP, 它是端口范围（port range）的起始端口号; 如果协议是ICMP, 它是ICMP类型（type）"
		type "Integer"
		since "0.6"
	}
	field {
		name "protocol"
		desc "流量协议类型"
		type "String"
		since "0.6"
	}
	field {
		name "state"
		desc "规则的可用状态, 当前版本未实现"
		type "String"
		since "0.6"
	}
	field {
		name "allowedCidr"
		desc "允许的CIDR,根据流量类型的不同, 允许的CIDR有不同的含义,如果流量类型是Ingress, 允许的CIDR是允许访问虚拟机网卡的源CIDR,如果流量类型是Egress, 允许的CIDR是允许从虚拟机网卡离开并到达的目的地CIDR"
		type "String"
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
}
