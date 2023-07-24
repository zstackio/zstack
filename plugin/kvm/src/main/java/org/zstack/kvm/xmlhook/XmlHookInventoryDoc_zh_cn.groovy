package org.zstack.kvm.xmlhook

import org.zstack.kvm.xmlhook.XmlHookType
import java.sql.Timestamp

doc {

	title "用户自定义xml hook"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "4.7.21"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "4.7.21"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "4.7.21"
	}
	ref {
		name "type"
		path "org.zstack.kvm.xmlhook.XmlHookInventory.type"
		desc "null"
		type "XmlHookType"
		since "4.7.21"
		clz XmlHookType.class
	}
	field {
		name "hookScript"
		desc "xml hook脚本内容"
		type "String"
		since "4.7.21"
	}
	field {
		name "libvirtVersion"
		desc ""
		type "String"
		since "4.7.21"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "4.7.21"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "4.7.21"
	}
}
