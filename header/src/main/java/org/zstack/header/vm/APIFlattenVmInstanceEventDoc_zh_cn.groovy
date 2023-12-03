package org.zstack.header.vm

import org.zstack.header.vm.VmInstanceInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "扁平合并云主机结果"

	ref {
		name "inventory"
		path "org.zstack.header.vm.APIFlattenVmInstanceEvent.inventory"
		desc "null"
		type "VmInstanceInventory"
		since "4.7.0"
		clz VmInstanceInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.7.0"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIFlattenVmInstanceEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.7.0"
		clz ErrorCode.class
	}
}
