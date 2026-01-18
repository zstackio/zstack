package org.zstack.header.core.external.service

import java.sql.Timestamp

doc {

	title "外部服务配置"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "5.4.6"
	}
	field {
		name "serviceType"
		desc "外部服务类型, 如 Prometheus2, FluentBitServer"
		type "String"
		since "5.4.6"
	}
	field {
		name "configuration"
		desc "外部服务配置, 使用 json 格式"
		type "String"
		since "5.4.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "5.4.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "5.4.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "5.4.6"
	}
}
