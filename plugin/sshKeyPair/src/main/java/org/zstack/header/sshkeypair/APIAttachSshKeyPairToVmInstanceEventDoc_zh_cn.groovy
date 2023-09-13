package org.zstack.header.sshkeypair

import org.zstack.header.sshkeypair.SshKeyPairInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "云主机挂载密钥对返回"

	ref {
		name "inventory"
		path "org.zstack.header.sshkeypair.APIAttachSshKeyPairToVmInstanceEvent.inventory"
		desc "null"
		type "SshKeyPairInventory"
		since "4.7.21"
		clz SshKeyPairInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.7.21"
	}
	ref {
		name "error"
		path "org.zstack.header.sshkeypair.APIAttachSshKeyPairToVmInstanceEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.7.21"
		clz ErrorCode.class
	}
}
