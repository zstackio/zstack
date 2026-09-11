package org.zstack.header.core.external.plugin

import org.zstack.abstraction.OptionType
import java.sql.Timestamp

doc {

	title "插件驱动器"

	field {
		name "uuid"
		desc "插件Driver UUID"
		type "String"
		since "5.3.28"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "5.3.0"
	}
	field {
		name "type"
		desc "资源类型"
		type "String"
		since "5.3.0"
	}
	field {
		name "vendor"
		desc "插件供应商"
		type "String"
		since "5.3.0"
	}
	field {
		name "features"
		desc "插件特性"
		type "String"
		since "5.3.0"
	}
	ref {
		name "optionTypes"
		path "org.zstack.header.core.external.plugin.PluginDriverInventory.optionTypes"
		desc "插件声明的动态配置字段"
		type "Collection"
		since "5.3.28"
		clz OptionType.class
	}
	field {
		name "deleted"
		desc "插件是否已被软删除"
		type "boolean"
		since "5.3.28"
	}
	field {
		name "license"
		desc "插件许可证"
		type "String"
		since "5.3.0"
	}
	field {
		name "version"
		desc "插件版本"
		type "String"
		since "5.3.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "5.3.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "5.3.28"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "5.3.28"
	}
}
