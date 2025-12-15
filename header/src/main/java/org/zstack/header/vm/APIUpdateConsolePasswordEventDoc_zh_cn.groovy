package org.zstack.header.vm

import org.zstack.header.vm.VmInstanceInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "更新虚拟机控制台密码事件"

	ref {
		name "inventory"
		path "org.zstack.header.vm.APIUpdateConsolePasswordEvent.inventory"
		desc "null"
		type "VmInstanceInventory"
		since "5.4.2"
		clz VmInstanceInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.4.2"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIUpdateConsolePasswordEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.4.2"
		clz ErrorCode.class
	}
}
