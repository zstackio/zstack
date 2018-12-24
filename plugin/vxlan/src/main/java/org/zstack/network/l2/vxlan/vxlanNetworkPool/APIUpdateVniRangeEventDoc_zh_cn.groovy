package org.zstack.network.l2.vxlan.vxlanNetworkPool

import org.zstack.header.errorcode.ErrorCode

doc {

	title "修改 Vni Range"

	ref {
		name "error"
		path "org.zstack.network.l2.vxlan.vxlanNetworkPool.APIUpdateVniRangeEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.3.0"
		clz ErrorCode.class
	}
}
