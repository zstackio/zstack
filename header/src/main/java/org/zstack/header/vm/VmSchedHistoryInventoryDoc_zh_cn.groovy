package org.zstack.header.vm

import java.lang.Boolean
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "虚拟机调度历史清单"

	field {
		name "vmInstanceUuid"
		desc "云主机UUID"
		type "String"
		since "4.4.24"
	}
	field {
		name "accountUuid"
		desc "账户UUID"
		type "String"
		since "4.4.24"
	}
	field {
		name "schedType"
		desc "调度原因"
		type "String"
		since "4.4.24"
	}
	field {
		name "reason"
		desc "调度的详情，具体说明为什么进行虚拟机的调度"
		type "String"
		since "zsv 4.1.6"
	}
	field {
		name "success"
		desc "是否成功"
		type "Boolean"
		since "4.4.24"
	}
	field {
		name "lastHostUuid"
		desc ""
		type "上次所在物理机UUID"
		since "4.4.24"
	}
	field {
		name "destHostUuid"
		desc "目标物理机UUID"
		type "String"
		since "4.4.24"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "4.4.24"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "4.4.24"
	}
}
