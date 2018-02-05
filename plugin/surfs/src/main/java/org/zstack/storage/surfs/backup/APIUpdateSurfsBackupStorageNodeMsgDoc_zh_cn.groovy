package org.zstack.storage.surfs.backup

import org.zstack.storage.surfs.backup.APIUpdateNodeToSurfsBackupStorageEvent

doc {
    title "UpdateSurfsBackupStorageNode"

    category "未知类别"

    desc """在这里填写API描述"""

    rest {
        request {
			url "PUT /v1/backup-storage/surfs/nodes/{nodeUuid}/actions"


            header(Authorization: 'OAuth the-session-uuid')

            clz APIUpdateSurfsBackupStorageNodeMsg.class

            desc """"""
            
			params {

				column {
					name "nodeUuid"
					enclosedIn "updateSurfsBackupStorageNode"
					desc ""
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "hostname"
					enclosedIn "updateSurfsBackupStorageNode"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "sshUsername"
					enclosedIn "updateSurfsBackupStorageNode"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "sshPassword"
					enclosedIn "updateSurfsBackupStorageNode"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "sshPort"
					enclosedIn "updateSurfsBackupStorageNode"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "0.6"
					
				}
				column {
					name "nodePort"
					enclosedIn "updateSurfsBackupStorageNode"
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
            clz APIUpdateNodeToSurfsBackupStorageEvent.class
        }
    }
}