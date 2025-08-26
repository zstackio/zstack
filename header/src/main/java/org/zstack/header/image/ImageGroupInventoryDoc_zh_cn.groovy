package org.zstack.header.image

import java.lang.Integer
import java.sql.Timestamp

doc {

	title "镜像组清单"

	field {
		name "imageCount"
		desc ""
		type "Integer"
		since "5.4.0"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "5.4.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "5.4.0"
	}
	field {
		name "status"
		desc ""
		type "String"
		since "5.4.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "5.4.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "5.4.0"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "5.4.0"
	}
}
