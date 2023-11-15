package org.zstack.header.agent.versioncontrol

import org.zstack.header.agent.versioncontrol.AgentVersionInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询 Agent 版本信息返回"

	ref {
		name "inventories"
		path "org.zstack.header.agent.versioncontrol.APIQueryAgentVersionReply.inventories"
		desc "null"
		type "List"
		since "5.0.0"
		clz AgentVersionVO.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.0.0"
	}
	ref {
		name "error"
		path "org.zstack.header.agent.versioncontrol.APIQueryAgentVersionReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.0.0"
		clz ErrorCode.class
	}
}
