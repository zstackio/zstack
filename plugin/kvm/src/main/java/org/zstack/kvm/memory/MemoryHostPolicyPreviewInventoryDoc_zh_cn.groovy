package org.zstack.kvm.memory

import org.zstack.kvm.memory.MemoryBlockedPolicyFieldInventory

doc {

	title "Host策略预览结果"

	field {
		name "hostUuid"
		desc "物理机UUID"
		type "String"
		since "5.5.38"
	}
	field {
		name "eligible"
		desc "当前Host是否具备应用所提议策略的资格。"
		type "boolean"
		since "5.5.38"
	}
	ref {
		name "blockedFields"
		path "org.zstack.kvm.memory.MemoryHostPolicyPreviewInventory.blockedFields"
		desc "不符合资格的策略字段及稳定原因码。"
		type "List"
		since "5.5.38"
		clz MemoryBlockedPolicyFieldInventory.class
	}
}
