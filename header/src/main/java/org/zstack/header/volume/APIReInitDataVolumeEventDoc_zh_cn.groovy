package org.zstack.header.volume

import org.zstack.header.volume.VolumeInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "重新初始化数据云盘事件"

	ref {
		name "inventory"
		path "org.zstack.header.volume.APIReInitDataVolumeEvent.inventory"
		desc "重新初始化后的数据云盘清单"
		type "VolumeInventory"
		since "5.4.2"
		clz VolumeInventory.class
	}
	field {
		name "success"
		desc "操作是否成功"
		type "boolean"
		since "5.4.2"
	}
	ref {
		name "error"
		path "org.zstack.header.volume.APIReInitDataVolumeEvent.error"
		desc "错误码，若不为null，则表示操作失败，操作成功时该字段为null"
		type "ErrorCode"
		since "5.4.2"
		clz ErrorCode.class
	}
}
