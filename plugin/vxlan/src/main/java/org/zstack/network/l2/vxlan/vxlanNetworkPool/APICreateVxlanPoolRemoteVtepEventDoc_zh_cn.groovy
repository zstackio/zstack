package org.zstack.network.l2.vxlan.vxlanNetworkPool

import org.zstack.network.l2.vxlan.vtep.RemoteVtepInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "创建remote vxlan隧道端点清单"

	ref {
		name "inventory"
		path "org.zstack.network.l2.vxlan.vxlanNetworkPool.APICreateVxlanPoolRemoteVtepEvent.inventory"
		desc "null"
		type "RemoteVtepInventory"
		since "4.7.11"
		clz RemoteVtepInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.7.11"
	}
	ref {
		name "error"
		path "org.zstack.network.l2.vxlan.vxlanNetworkPool.APICreateVxlanPoolRemoteVtepEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.7.11"
		clz ErrorCode.class
	}
}
