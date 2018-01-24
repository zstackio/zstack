package org.zstack.storage.surfs.primary

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
		name "fsid"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "clsname"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "clsdisplayname"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "isrootcls"
		desc ""
		type "boolean"
		since "0.6"
	}
	field {
		name "isactive"
		desc ""
		type "boolean"
		since "0.6"
	}
	field {
		name "totalCapacity"
		desc ""
		type "long"
		since "0.6"
	}
	field {
		name "availableCapacity"
		desc ""
		type "long"
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
