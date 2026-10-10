package org.zstack.kvm.memory



doc {

	title "策略字段能力"

	field {
		name "readable"
		desc "客户端是否可读取该字段。"
		type "boolean"
		since "5.5.38"
	}
	field {
		name "writable"
		desc "当前来源和能力下该字段是否可写；不构成授权令牌。"
		type "boolean"
		since "5.5.38"
	}
	field {
		name "basicUi"
		desc "是否适合展示在基础UI。"
		type "boolean"
		since "5.5.38"
	}
	field {
		name "type"
		desc "字段值的数据类型。"
		type "String"
		since "5.5.38"
	}
	field {
		name "unit"
		desc "字段单位；容量通常为bytes。"
		type "String"
		since "5.5.38"
	}
	field {
		name "effect"
		desc "字段生效影响说明。"
		type "String"
		since "5.5.38"
	}
	field {
		name "reasonCode"
		desc "不可读或不可写时的稳定原因码。"
		type "String"
		since "5.5.38"
	}
}
