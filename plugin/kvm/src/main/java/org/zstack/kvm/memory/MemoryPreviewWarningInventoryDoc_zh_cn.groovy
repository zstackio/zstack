package org.zstack.kvm.memory



doc {

	title "策略预览告警"

	field {
		name "code"
		desc "稳定机器可读告警码；客户端不应解析英文message。"
		type "String"
		since "5.5.38"
	}
	field {
		name "message"
		desc "当前语言环境下的告警文本。"
		type "String"
		since "5.5.38"
	}
	field {
		name "messageKey"
		desc "用于本地化的消息键。"
		type "String"
		since "5.5.38"
	}
	field {
		name "formatArgs"
		desc "消息模板格式化参数。"
		type "List"
		since "5.5.38"
	}
}
