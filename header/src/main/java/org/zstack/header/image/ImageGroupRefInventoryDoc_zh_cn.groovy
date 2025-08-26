package org.zstack.header.image

import java.sql.Timestamp

doc {

	title "镜像组引用结构清单"

	field {
		name "imageUuid"
		desc "镜像UUID"
		type "String"
		since "5.4.0"
	}
	field {
		name "imageGroupUuid"
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
}
