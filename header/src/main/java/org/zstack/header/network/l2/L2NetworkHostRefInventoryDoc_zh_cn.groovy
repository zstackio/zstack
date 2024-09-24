package org.zstack.header.network.l2

import java.sql.Timestamp

doc {

	title "二层网络物理机关联关系清单"

	field {
		name "hostUuid"
		desc "物理机UUID"
		type "String"
		since "4.1.0"
	}
	field {
		name "l2NetworkUuid"
		desc "二层网络UUID"
		type "String"
		since "4.1.0"
	}
	field {
		name "l2ProviderType"
		desc "二层网络实现类型"
		type "String"
		since "4.1.0"
	}
	field {
		name "bridgeName"
		desc "网桥名称"
		type "String"
		since "4.3.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "4.1.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "4.1.0"
	}
}
