package org.zstack.header.image

import java.lang.Long
import java.lang.Long
import java.lang.Boolean
import java.sql.Timestamp
import java.sql.Timestamp
import org.zstack.header.image.ImageBackupStorageRefInventory

doc {

	title "在这里输入结构的名称"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "0.6"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "0.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "0.6"
	}
	field {
		name "exportUrl"
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
		name "status"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "size"
		desc ""
		type "Long"
		since "0.6"
	}
	field {
		name "actualSize"
		desc ""
		type "Long"
		since "0.6"
	}
	field {
		name "md5Sum"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "url"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "mediaType"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "guestOsType"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "type"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "platform"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "format"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "system"
		desc ""
		type "Boolean"
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
	ref {
		name "backupStorageRefs"
		path "org.zstack.header.image.ImageInventory.backupStorageRefs"
		desc "null"
		type "List"
		since "0.6"
		clz ImageBackupStorageRefInventory.class
	}
}
