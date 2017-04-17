package org.zstack.network.l2.vxlan.vxlanNetworkPool

import org.zstack.header.errorcode.ErrorCode
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VniRangeInventory

doc {

	title "在这里输入结构的名称"

	ref {
		name "error"
		path "org.zstack.network.l2.vxlan.vxlanNetworkPool.APIQueryVniRangeReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.network.l2.vxlan.vxlanNetworkPool.APIQueryVniRangeReply.inventories"
		desc "null"
		type "List"
		since "0.6"
		clz VniRangeInventory.class
	}
}
