package org.zstack.authentication.checkfile

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "0.6"
	}
	field {
		name "path"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "node"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "category"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "hexType"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "digest"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "state"
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
