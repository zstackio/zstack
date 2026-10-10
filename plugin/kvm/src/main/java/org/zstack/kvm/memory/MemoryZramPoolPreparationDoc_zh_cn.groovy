package org.zstack.kvm.memory

doc {
	title "内存优化ZRAM池准备证据"

	field {
		name "expectedHostBootId"
		desc "读取池状态时的Host启动标识；提交时必须仍匹配当前Host启动标识。"
		type "String"
		since "5.5.38"
	}
	field {
		name "expectedPoolGeneration"
		desc "已排空并准备重置的ZRAM池代次，使用64位小写十六进制摘要。"
		type "String"
		since "5.5.38"
	}
	field {
		name "resetConfirmed"
		desc "显式确认仅在池状态及代次校验通过后重置现有ZRAM池；必须为true。"
		type "boolean"
		since "5.5.38"
	}
}
