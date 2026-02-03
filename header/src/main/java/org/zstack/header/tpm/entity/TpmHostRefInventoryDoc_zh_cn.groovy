package org.zstack.header.tpm.entity

import java.sql.Timestamp

doc {

	title "TPM 与主机的相关数据"

	field {
		name "id"
		desc "自增主键"
		type "long"
		since "5.0.0"
	}
	field {
		name "tpmUuid"
		desc "TPM UUID"
		type "String"
		since "5.0.0"
	}
	field {
		name "hostUuid"
		desc "主机 UUID"
		type "String"
		since "5.0.0"
	}
	field {
		name "path"
		desc "遗留 TPM 状态文件的位置"
		type "String"
		since "5.0.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "5.0.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "5.0.0"
	}
}
