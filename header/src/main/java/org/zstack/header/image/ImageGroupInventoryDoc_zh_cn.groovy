package org.zstack.header.image

import java.lang.Integer
import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

	field {
		name "imageCount"
		desc ""
		type "Integer"
		since "5.3.36"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "5.3.36"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "5.3.36"
	}
	field {
		name "status"
		desc ""
		type "String"
		since "5.3.36"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "5.3.36"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "5.3.36"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "5.3.36"
	}
}
