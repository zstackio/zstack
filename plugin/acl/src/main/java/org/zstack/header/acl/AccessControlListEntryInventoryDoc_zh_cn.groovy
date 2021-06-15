package org.zstack.header.acl

doc {

	title "访问控制策略组规则清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.9"
	}
	field {
		name "aclUuid"
		desc "访问控制策略组的唯一标识"
		type "String"
		since "3.9"
	}
	field {
		name "type"
		desc "访问控制策略组规则类型"
		type "String"
		since "4.1.3"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "4.1.3"
	}
	field {
		name "domain"
		desc "转发规则的域名"
		type "String"
		since "4.1.3"
	}
	field {
		name "url"
		desc "转发规则的路径"
		type "String"
		since "4.1.3"
	}
	field {
		name "ipEntries"
		desc "IP组"
		type "String"
		since "3.9"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "3.9"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.9"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.9"
	}
}
