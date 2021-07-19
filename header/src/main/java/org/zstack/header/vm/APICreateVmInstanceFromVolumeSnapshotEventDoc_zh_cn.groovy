package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.vm.VmInstanceInventory

doc {

	title "从快照创建云主机结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APICreateVmInstanceFromVolumeSnapshotEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.1.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.vm.APICreateVmInstanceFromVolumeSnapshotEvent.inventory"
		desc "null"
		type "VmInstanceInventory"
		since "4.1.0"
		clz VmInstanceInventory.class
	}
}
