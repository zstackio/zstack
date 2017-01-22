package org.zstack.header.network.l2

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.network.l2.L2VlanNetworkInventory

doc {

	title "二层Vlan网络清单"

	ref {
		name "error"
		path "org.zstack.header.network.l2.APIQueryL2VlanNetworkReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.network.l2.APIQueryL2VlanNetworkReply.inventories"
		desc "null"
		type "List"
		since "0.6"
		clz L2VlanNetworkInventory.class
	}
}
