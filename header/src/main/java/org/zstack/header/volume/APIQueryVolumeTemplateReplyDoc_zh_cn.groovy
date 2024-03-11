package org.zstack.header.volume

import org.zstack.header.volume.VolumeTemplateInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询硬盘模板的返回"

	ref {
		name "inventories"
		path "org.zstack.header.volume.APIQueryVolumeTemplateReply.inventories"
		desc "null"
		type "List"
		since "zsv 4.2.0"
		clz VolumeTemplateInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 4.2.0"
	}
	ref {
		name "error"
		path "org.zstack.header.volume.APIQueryVolumeTemplateReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 4.2.0"
		clz ErrorCode.class
	}
}
