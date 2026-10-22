package org.zstack.network.securitygroup

import java.lang.Integer

doc {

	title "安全组模板规则"

	field {
		name "priority"
		desc "同方向规则的顺序"
		type "Integer"
		since "5.7.0"
	}
	field {
		name "type"
		desc "规则方向：Ingress 或 Egress"
		type "String"
		since "5.7.0"
	}
	field {
		name "state"
		desc "规则状态"
		type "String"
		since "5.7.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "5.7.0"
	}
	field {
		name "remoteSecurityGroupUuid"
		desc "引用的安全组 UUID"
		type "String"
		since "5.7.0"
	}
	field {
		name "ipVersion"
		desc "IP 版本"
		type "Integer"
		since "5.7.0"
	}
	field {
		name "protocol"
		desc "协议"
		type "String"
		since "5.7.0"
	}
	field {
		name "srcIpRange"
		desc "源 IP 范围"
		type "String"
		since "5.7.0"
	}
	field {
		name "dstIpRange"
		desc "目的 IP 范围"
		type "String"
		since "5.7.0"
	}
	field {
		name "dstPortRange"
		desc "目的端口范围"
		type "String"
		since "5.7.0"
	}
	field {
		name "action"
		desc "规则动作"
		type "String"
		since "5.7.0"
	}
	field {
		name "startPort"
		desc "起始端口"
		type "Integer"
		since "5.7.0"
	}
	field {
		name "endPort"
		desc "结束端口"
		type "Integer"
		since "5.7.0"
	}
	field {
		name "allowedCidr"
		desc "允许的 CIDR"
		type "String"
		since "5.7.0"
	}
}
