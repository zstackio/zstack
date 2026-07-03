package org.zstack.header.storage.primary

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.volume.VolumeInventory

doc {

	title "云盘清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "5.5.28"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.primary.APIChangeVolumeProtocolEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.5.28"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.storage.primary.APIChangeVolumeProtocolEvent.inventory"
		desc "云盘清单"
		type "VolumeInventory"
		since "5.5.28"
		clz VolumeInventory.class
	}
}
