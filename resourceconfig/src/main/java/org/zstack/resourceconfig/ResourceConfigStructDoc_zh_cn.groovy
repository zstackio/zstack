package org.zstack.resourceconfig

import org.zstack.resourceconfig.ResourceConfigInventory

doc {

	title "资源级配置的数据结构"

	field {
		name "value"
		desc ""
		type "String"
		since "4.7.0"
	}
	ref {
		name "effectiveConfigs"
		path "org.zstack.resourceconfig.ResourceConfigStruct.effectiveConfigs"
		desc "null"
		type "List"
		since "4.7.0"
		clz ResourceConfigInventory.class
	}
	field {
		name "name"
		desc ""
		type "String"
		since "4.7.0"
	}
}
