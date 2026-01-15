package org.zstack.header.storage.primary

import org.zstack.header.errorcode.ErrorCode

doc {

	title "检查存储一致性返回"

	field {
		name "consistent"
		desc "是否一致"
		type "boolean"
		since "5.0.0"
	}
	field {
		name "status"
		desc "一致性检查结果: CONSISTENT（VG 存在且 UUID 一致）/ UUID_MISMATCH（VG 存在但 UUID 不一致，可执行接管）/ VG_NOT_FOUND（未找到 WWID 匹配的 VG）"
		type "ConsistencyCheckStatus"
		since "5.0.0"
	}
	field {
		name "candidateVgUuid"
		desc "status 为 UUID_MISMATCH 时，存储上实际找到的 VG UUID（即接管候选）；其他情况为 null"
		type "String"
		since "5.0.0"
	}
	field {
		name "success"
		desc "操作是否成功"
		type "boolean"
		since "5.0.0"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.primary.APICheckPrimaryStorageConsistencyReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "5.0.0"
		clz ErrorCode.class
	}
}
