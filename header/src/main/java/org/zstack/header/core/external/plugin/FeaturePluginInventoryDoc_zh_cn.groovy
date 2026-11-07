package org.zstack.header.core.external.plugin

import java.lang.Integer

doc {

	title "功能插件"

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
		name "status"
		desc "插件状态：Loaded（已加载）、Failed（加载失败）、Incompatible（与当前平台不兼容）、PackageMissing（缺少插件包）、Unlicensed（未获许可）"
		type "String"
		since "5.5.48"
	}
	field {
		name "error"
		desc "插件未能加载的原因"
		type "String"
		since "5.5.48"
	}
	field {
		name "sha256"
		desc "插件包的SHA256校验值"
		type "String"
		since "5.5.48"
	}
	field {
		name "licenseAddon"
		desc "插件需要的许可证模块，为空表示不需要许可证"
		type "String"
		since "5.5.48"
	}
	field {
		name "deployingHosts"
		desc "插件的物理机部分推送失败、将在物理机重连时补装的已连接物理机数量"
		type "Integer"
		since "5.5.48"
	}
}
