package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.vm.VmNicInventory

doc {

	title "网卡清单"

	ref {
		name "error"
		path "org.zstack.header.vm.APIChangeVmNicNetworkEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.1.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.vm.APIChangeVmNicNetworkEvent.inventory"
		desc "null"
		type "VmNicInventory"
		since "4.1.0"
		clz VmNicInventory.class
	}
}
