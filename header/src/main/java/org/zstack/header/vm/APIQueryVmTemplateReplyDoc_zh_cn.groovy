package org.zstack.header.vm

import org.zstack.header.vm.VmTemplateInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询虚拟机模板的返回"

	ref {
		name "inventories"
		path "org.zstack.header.vm.APIQueryVmTemplateReply.inventories"
		desc "null"
		type "List"
		since "zsv 4.2.0"
		clz VmTemplateInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 4.2.0"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIQueryVmTemplateReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 4.2.0"
		clz ErrorCode.class
	}
}
