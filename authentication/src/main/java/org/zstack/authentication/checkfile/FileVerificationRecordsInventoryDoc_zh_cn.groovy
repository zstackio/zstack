package org.zstack.authentication.checkfile

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

	field {
		name "id"
		desc ""
		type "long"
		since "0.6"
	}
	field {
		name "fileVerificationUuid"
		desc ""
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
		name "currentDigest"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "targetDigest"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "reason"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "recoverFlag"
		desc ""
		type "boolean"
		since "0.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "0.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "0.6"
	}
}
