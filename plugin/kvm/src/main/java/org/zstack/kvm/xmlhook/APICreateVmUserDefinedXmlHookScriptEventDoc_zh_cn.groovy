package org.zstack.kvm.xmlhook

import org.zstack.kvm.xmlhook.XmlHookInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "创建用户自定义xml hook返回"

	ref {
		name "inventory"
		path "org.zstack.kvm.xmlhook.APCreateVmUserDefinedXmlHookScriptEvent.inventory"
		desc "null"
		type "XmlHookInventory"
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
		path "org.zstack.kvm.xmlhook.APCreateVmUserDefinedXmlHookScriptEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.7.21"
		clz ErrorCode.class
	}
}
