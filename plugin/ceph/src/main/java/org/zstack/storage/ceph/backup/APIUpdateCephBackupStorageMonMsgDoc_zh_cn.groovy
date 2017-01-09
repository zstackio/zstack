package org.zstack.storage.ceph.backup

import org.zstack.storage.ceph.backup.APIUpdateCephBackupStorageMonEvent

doc {
    title "UpdateCephBackupStorageMon"

    category "未知类别"

    desc "在这里填写API描述"

    rest {
        request {
			url "PUT /v1/backup-storage/ceph/mons/{monUuid}/actions"


            header (OAuth: 'the-session-uuid')

            clz APIUpdateCephBackupStorageMonMsg.class

            desc ""
            
			params {

				column {
					name "monUuid"
					enclosedIn "updateCephBackupStorageMon"
					desc ""
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "hostname"
					enclosedIn "updateCephBackupStorageMon"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "sshUsername"
					enclosedIn "updateCephBackupStorageMon"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "sshPassword"
					enclosedIn "updateCephBackupStorageMon"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "sshPort"
					enclosedIn "updateCephBackupStorageMon"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "0.6"
					
				}
				column {
					name "monPort"
					enclosedIn "updateCephBackupStorageMon"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIUpdateCephBackupStorageMonEvent.class
        }
    }
}