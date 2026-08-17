package org.zstack.header.identity

import java.sql.Timestamp

doc {

	title "会话清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "5.4.10"
	}
	field {
		name "accountUuid"
		desc "账户UUID"
		type "String"
		since "5.4.10"
	}
	field {
		name "userUuid"
		desc "用户UUID"
		type "String"
		since "5.4.10"
	}
	field {
		name "userType"
		desc "用户类型"
		type "String"
		since "5.4.10"
	}
	field {
		name "expiredDate"
		desc ""
		type "Timestamp"
		since "5.4.10"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "5.4.10"
	}
}
