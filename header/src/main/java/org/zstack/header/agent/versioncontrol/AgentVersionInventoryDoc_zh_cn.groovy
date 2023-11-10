package org.zstack.header.agent.versioncontrol

import java.sql.Timestamp

doc {

	title "Agent 版本信息"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "5.0.0"
	}
	field {
		name "agentType"
		desc "Agent 类型"
		type "String"
		since "5.0.0"
	}
	field {
		name "currentVersion"
		desc "当前版本"
		type "String"
		since "5.0.0"
	}
	field {
		name "expectVersion"
		desc "预期版本"
		type "String"
		since "5.0.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "5.0.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "5.0.0"
	}
}
