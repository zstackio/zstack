package org.zstack.header.image

import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

	field {
		name "imageUuid"
		desc "镜像UUID"
		type "String"
		since "5.3.36"
	}
	field {
		name "imageGroupUuid"
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
}
