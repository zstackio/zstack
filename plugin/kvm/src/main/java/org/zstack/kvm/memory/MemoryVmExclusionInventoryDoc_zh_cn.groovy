package org.zstack.kvm.memory



doc {

	title "VM迁移不参与证据"

	field {
		name "sourceHostUuid"
		desc "迁移前源Host UUID。"
		type "String"
		since "5.5.38"
	}
	field {
		name "targetHostUuid"
		desc "迁移后目标Host UUID。"
		type "String"
		since "5.5.38"
	}
	field {
		name "sourceRevisions"
		desc "来源revision证据JSON字符串。"
		type "String"
		since "5.5.38"
	}
	field {
		name "sourceInstanceGeneration"
		desc "迁移时VM实例代次证据。"
		type "String"
		since "5.5.38"
	}
	field {
		name "retained"
		desc "true表示迁移保留了不参与配置；不是实时资格结论。"
		type "boolean"
		since "5.5.38"
	}
}
