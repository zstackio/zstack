package org.zstack.header.storage.primary

import org.zstack.header.storage.primary.APITakeoverPrimaryStorageEvent

doc {
	title "TakeoverPrimaryStorage"

	category "storage.primary"

	desc """接管主存储。将其他ZStack平台的共享块主存储接管到当前平台。
前置条件：主存储必须通过APICheckPrimaryStorageConsistencyMsg检查且consistent=false。
接管操作不可逆（agent侧会执行VG rename、PV UUID reset、sanlock lockspace reset）。
接管完成后自动触发reconnect，通过返回的reconnectResult/reconnectError获取重连状态。"""

	rest {
		request {
			url "PUT /v1/primary-storage/{uuid}/takeover"

			header (Authorization: 'OAuth the-session-uuid')

			clz APITakeoverPrimaryStorageMsg.class

			desc """接管指定主存储"""

			params {

				column {
					name "uuid"
					enclosedIn "takeoverPrimaryStorage"
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
					location "body"
					type "List"
					optional true
					since "5.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.0.0"
				}
			}
		}

		response {
			clz APITakeoverPrimaryStorageEvent.class
		}
	}
}