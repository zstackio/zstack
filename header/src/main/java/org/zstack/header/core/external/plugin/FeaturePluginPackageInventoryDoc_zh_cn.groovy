package org.zstack.header.core.external.plugin

import java.lang.Boolean

doc {

	title "功能插件包"

	field {
		name "path"
		desc "插件包在插件仓库中的路径"
		type "String"
		since "5.5.48"
	}
	field {
		name "source"
		desc "插件包来源：iso（随安装介质提供）或upload（用户上传）"
		type "String"
		since "5.5.48"
	}
	field {
		name "id"
		desc "插件ID"
		type "String"
		since "5.5.48"
	}
	field {
		name "name"
		desc "插件名称"
		type "String"
		since "5.5.48"
	}
	field {
		name "category"
		desc "插件分类，如network"
		type "String"
		since "5.5.48"
	}
	field {
		name "version"
		desc "插件版本"
		type "String"
		since "5.5.48"
	}
	field {
		name "installed"
		desc "该插件（任一版本）是否已安装"
		type "Boolean"
		since "5.5.48"
	}
	field {
		name "sha256"
		desc "插件包的SHA256校验值"
		type "String"
		since "5.5.48"
	}
	field {
		name "desired"
		desc "已安装的是否正是这个插件包"
		type "Boolean"
		since "5.5.48"
	}
	field {
		name "licenseAddon"
		desc "插件需要的许可证模块，为空表示不需要许可证"
		type "String"
		since "5.5.48"
	}
	field {
		name "error"
		desc "插件包无法读取或校验不通过的原因"
		type "String"
		since "5.5.48"
	}
}
