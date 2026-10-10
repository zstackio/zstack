package org.zstack.kvm.memory

import org.zstack.header.errorcode.ErrorCode
import org.zstack.kvm.memory.MemoryPolicyInventory
import org.zstack.kvm.memory.MemoryPreviewWarningInventory
import org.zstack.kvm.memory.MemoryHostPolicyPreviewInventory

doc {

	title "内存策略预览响应"

	field {
		name "success"
		desc "API调用是否成功。"
		type "boolean"
		since "5.5.38"
	}
	ref {
		name "error"
		path "org.zstack.kvm.memory.APIPreviewMemoryPolicyReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.5.38"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.kvm.memory.APIPreviewMemoryPolicyReply.inventory"
		desc "策略预览inventory；预览本身不提交配置。"
		type "MemoryPolicyInventory"
		since "5.5.38"
		clz MemoryPolicyInventory.class
	}
	field {
		name "warnings"
		desc "兼容旧客户端的告警文本列表。"
		type "List"
		since "5.5.38"
	}
	ref {
		name "warningDetails"
		path "org.zstack.kvm.memory.APIPreviewMemoryPolicyReply.warningDetails"
		desc "结构化告警，包含稳定code、本地化message和格式参数。"
		type "List"
		since "5.5.38"
		clz MemoryPreviewWarningInventory.class
	}
	field {
		name "hostUuids"
		desc "参与本次预览的Host UUID列表。"
		type "List"
		since "5.5.38"
	}
	ref {
		name "hostResults"
		path "org.zstack.kvm.memory.APIPreviewMemoryPolicyReply.hostResults"
		desc "每个Host的资格判断和阻塞字段；列表为空不代表未请求预览。"
		type "List"
		since "5.5.38"
		clz MemoryHostPolicyPreviewInventory.class
	}
}
