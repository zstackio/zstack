package org.zstack.header.image

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

	field {
		name "imageUuid"
		desc "镜像UUID"
		type "String"
		since "0.6"
	}
	field {
		name "backupStorageUuid"
		desc "镜像存储UUID"
		type "String"
		since "0.6"
	}
	field {
		name "installPath"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "status"
		desc ""
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
