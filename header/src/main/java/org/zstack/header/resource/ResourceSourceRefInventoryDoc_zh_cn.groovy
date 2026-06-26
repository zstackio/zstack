package org.zstack.header.resource

import java.sql.Timestamp

doc {

	title "资源来源引用"

	field {
		name "uuid"
		desc "引用记录UUID"
		type "String"
		since "5.5.28"
	}
	field {
		name "resourceUuid"
		desc "资源UUID"
		type "String"
		since "5.5.28"
	}
	field {
		name "resourceType"
		desc "Cloud 侧资源类型，例如 IAM2VirtualIDVO、IAM2VirtualIDGroupVO、IAM2OrganizationVO、IAM2ProjectVO"
		type "String"
		since "5.5.28"
	}
	field {
		name "sourceType"
		desc "资源来源类型，ZIAM SCIM 同步资源固定为 ZIAM"
		type "String"
		since "5.5.28"
	}
	field {
		name "sourceName"
		desc "资源来源名称，ZIAM SCIM 同步资源固定为 ziam"
		type "String"
		since "5.5.28"
	}
	field {
		name "externalUuid"
		desc "上游身份源中的资源UUID"
		type "String"
		since "5.5.28"
	}
	field {
		name "externalType"
		desc "上游身份源中的资源类型"
		type "String"
		since "5.5.28"
	}
	field {
		name "syncType"
		desc "同步类型，SCIM 同步资源固定为 SCIM"
		type "String"
		since "5.5.28"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "5.5.28"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "5.5.28"
	}
}
