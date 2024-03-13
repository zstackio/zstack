package org.zstack.header.vm

import org.zstack.header.vm.VmInstanceInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "虚拟机模板转换为虚拟机"

	ref {
		name "inventory"
		path "org.zstack.header.vm.APIConvertVmTemplateToVmInstanceEvent.inventory"
		desc "虚拟机实例"
		type "VmInstanceInventory"
		since "zsv 4.2.0"
		clz VmInstanceInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 4.2.0"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIConvertVmTemplateToVmInstanceEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 4.2.0"
		clz ErrorCode.class
	}
}
