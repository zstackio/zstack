package org.zstack.header.storage.primary

import org.zstack.header.storage.primary.APICheckPrimaryStorageConsistencyReply

doc {
	title "CheckPrimaryStorageConsistency"

	category "storage.primary"

	desc """检查存储一致性"""

	rest {
		request {
			url "GET /v1/primary-storage/{uuid}/consistency"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICheckPrimaryStorageConsistencyMsg.class

			desc """检查指定主存储的一致性状态"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "主存储的UUID"
					location "url"
					type "String"
					optional false
					since "5.0.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "5.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "5.0.0"
				}
			}
		}

		response {
			clz APICheckPrimaryStorageConsistencyReply.class
		}
	}
}