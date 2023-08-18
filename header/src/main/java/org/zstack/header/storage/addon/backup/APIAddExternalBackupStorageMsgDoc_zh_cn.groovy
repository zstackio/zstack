package org.zstack.header.storage.addon.backup

import org.zstack.header.storage.addon.backup.APIAddExternalBackupStorageEvent

doc {
    title "AddExternalBackupStorage"

    category "storage.backup"

    desc """在这里填写API描述"""

    rest {
        request {
			url "POST /v1/backup-storage/addon"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIAddExternalBackupStorageMsg.class

            desc """"""
            
			params {

				column {
					name "identity"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "4.7.11"

				}
				column {
					name "url"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "4.7.11"

				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "4.7.11"

				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "4.7.11"

				}
				column {
					name "type"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "4.7.11"

				}
				column {
					name "importImages"
					enclosedIn "params"
					desc ""
					location "body"
					type "boolean"
					optional true
					since "4.7.11"

				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "4.7.11"

				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "4.7.11"

				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.7.11"

				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.7.11"

				}
			}
        }

        response {
            clz APIAddExternalBackupStorageEvent.class
        }
    }
}