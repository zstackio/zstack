package org.zstack.kvm.xmlhook

import org.zstack.kvm.xmlhook.XmlHookInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询用户自定义xml hook脚本返回"

	ref {
		name "inventories"
		path "org.zstack.kvm.xmlhook.APIQueryVmUserDefinedXmlHookScriptReply.inventories"
		desc "null"
		type "List"
		since "4.7.21"
		clz XmlHookInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.7.21"
	}
	ref {
		name "error"
		path "org.zstack.kvm.xmlhook.APIQueryVmUserDefinedXmlHookScriptReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.7.21"
		clz ErrorCode.class
	}
}
