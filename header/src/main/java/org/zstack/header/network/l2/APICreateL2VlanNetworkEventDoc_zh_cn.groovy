package org.zstack.header.network.l2

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.network.l2.L2VlanNetworkInventory

doc {

	title "二层网络清单"

	ref {
		name "error"
		path "org.zstack.header.network.l2.APICreateL2VlanNetworkEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.network.l2.APICreateL2VlanNetworkEvent.inventory"
		desc "null"
		type "L2VlanNetworkInventory"
		since "0.6"
		clz L2VlanNetworkInventory.class
	}
}
