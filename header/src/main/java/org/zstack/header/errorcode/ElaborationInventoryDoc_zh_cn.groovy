package org.zstack.header.errorcode 

doc {

	title "错误清单"

	field {
		name "id"
		desc "错误id"
		type "long"
		since "3.3.0"
	}
	field {
		name "errorInfo"
		desc "错误内容(截取结果)"
		type "String"
		since "3.3.0"
	}
	field {
		name "md5sum"
		desc "错误内容的md5码"
		type "String"
		since "3.3.0"
	}
	field {
		name "distance"
		desc "最佳匹配值"
		type "double"
		since "3.3.0"
	}
	field {
		name "matched"
		desc "是否命中错误码"
		type "boolean"
		since "3.3.0"
	}
	field {
		name "repeats"
		desc "出现次数"
		type "long"
		since "3.3.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.3.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.3.0"
	}
}
