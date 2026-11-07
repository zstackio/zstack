package org.zstack.header.core.external.plugin

import org.zstack.header.errorcode.ErrorCode

doc {

	title "卸载功能插件"

	field {
		name "success"
		desc "操作是否成功"
		type "boolean"
		since "5.5.48"
	}
	ref {
		name "error"
		path "org.zstack.header.core.external.plugin.APIUninstallFeaturePluginEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.5.48"
		clz ErrorCode.class
	}
}
