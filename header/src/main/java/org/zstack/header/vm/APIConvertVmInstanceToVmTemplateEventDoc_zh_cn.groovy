package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.vm.VmTemplateInventory
import org.zstack.header.volume.VolumeTemplateInventory

doc {

	title "虚拟机转换为虚拟机模板"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 4.2.0"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIConvertVmInstanceToVmTemplateEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 4.2.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.vm.APIConvertVmInstanceToVmTemplateEvent.inventory"
		desc "虚拟机模板"
		type "VmTemplateInventory"
		since "zsv 4.2.0"
		clz VmTemplateInventory.class
	}
}
