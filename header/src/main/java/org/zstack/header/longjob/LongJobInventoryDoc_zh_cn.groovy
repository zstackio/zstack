package org.zstack.header.longjob

import org.zstack.header.longjob.LongJobState
import java.sql.Timestamp
import java.lang.Long

doc {

	title "在这里输入结构的名称"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "5.5.16"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "5.5.16"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "5.5.16"
	}
	field {
		name "apiId"
		desc ""
		type "String"
		since "5.5.16"
	}
	field {
		name "jobName"
		desc ""
		type "String"
		since "5.5.16"
	}
	field {
		name "jobData"
		desc ""
		type "String"
		since "5.5.16"
	}
	field {
		name "jobResult"
		desc ""
		type "String"
		since "5.5.16"
	}
	ref {
		name "state"
		path "org.zstack.header.longjob.LongJobInventory.state"
		desc "null"
		type "LongJobState"
		since "5.5.16"
		clz LongJobState.class
	}
	field {
		name "targetResourceUuid"
		desc ""
		type "String"
		since "5.5.16"
	}
	field {
		name "managementNodeUuid"
		desc ""
		type "String"
		since "5.5.16"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "5.5.16"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "5.5.16"
	}
	field {
		name "executeTime"
		desc ""
		type "Long"
		since "5.5.16"
	}
}
