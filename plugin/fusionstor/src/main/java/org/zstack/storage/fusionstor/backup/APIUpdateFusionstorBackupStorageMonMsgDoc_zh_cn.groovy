package org.zstack.storage.fusionstor.backup

import org.zstack.storage.fusionstor.backup.APIUpdateMonToFusionstorBackupStorageEvent

doc {
    title "UpdateFusionstorBackupStorageMon"

    category "未知类别"

    desc """在这里填写API描述"""

    rest {
        request {
			url "PUT /v1/backup-storage/fusionstor/mons/{monUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')


            clz APIUpdateFusionstorBackupStorageMonMsg.class

            desc """"""
            
			params {

				column {
					name "monUuid"
					enclosedIn "updateFusionstorBackupStorageMon"
					desc ""
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "hostname"
					enclosedIn "updateFusionstorBackupStorageMon"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "sshUsername"
					enclosedIn "updateFusionstorBackupStorageMon"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "sshPassword"
					enclosedIn "updateFusionstorBackupStorageMon"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "sshPort"
					enclosedIn "updateFusionstorBackupStorageMon"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "0.6"
					
				}
				column {
					name "monPort"
					enclosedIn "updateFusionstorBackupStorageMon"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIUpdateMonToFusionstorBackupStorageEvent.class
        }
    }
}