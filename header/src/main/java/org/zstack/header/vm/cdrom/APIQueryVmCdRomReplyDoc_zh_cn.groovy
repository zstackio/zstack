package org.zstack.header.vm.cdrom

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.vm.cdrom.VmCdRomInventory

doc {

	title "查询CDROM清单返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.cdrom.APIQueryVmCdRomReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.3"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.vm.cdrom.APIQueryVmCdRomReply.inventories"
		desc "null"
		type "List"
		since "3.3"
		clz VmCdRomInventory.class
	}
}
