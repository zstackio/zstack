package org.zstack.header.core.progress

import java.lang.Long
import org.zstack.header.core.progress.TaskProgressInventory
import java.lang.Integer
import java.lang.Integer

doc {

	title "在这里输入结构的名称"

	field {
		name "taskUuid"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "taskName"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "parentUuid"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "type"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "content"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "opaque"
		desc ""
		type "LinkedHashMap"
		since "0.6"
	}
	field {
		name "time"
		desc ""
		type "Long"
		since "0.6"
	}
	ref {
		name "subTasks"
		path "org.zstack.header.core.progress.TaskProgressInventory.subTasks"
		desc "null"
		type "List"
		since "0.6"
		clz TaskProgressInventory.class
	}
	field {
		name "totalSteps"
		desc ""
		type "Integer"
		since "0.6"
	}
	field {
		name "currentStep"
		desc ""
		type "Integer"
		since "0.6"
	}
}
