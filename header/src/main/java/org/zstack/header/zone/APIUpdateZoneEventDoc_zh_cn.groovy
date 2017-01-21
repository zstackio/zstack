package org.zstack.header.zone

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.zone.ZoneInventory

doc {

	title "区域（Zone）清单"

	ref {
		name "error"
		path "org.zstack.header.zone.APIUpdateZoneEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.zone.APIUpdateZoneEvent.inventory"
		desc "null"
		type "ZoneInventory"
		since "0.6"
		clz ZoneInventory.class
	}
}
