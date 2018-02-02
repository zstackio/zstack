package org.zstack.storage.surfs.backup

import org.zstack.storage.surfs.backup.APIAddNodeToSurfsBackupStorageEvent

doc {
    title "AddNodeToSurfsBackupStorage"

    category "未知类别"

    desc """在这里填写API描述"""

    rest {
        request {
			url "POST /v1/backup-storage/surfs/{uuid}/nodes"


            header(Authorization: 'OAuth the-session-uuid')

            clz APIAddNodeToSurfsBackupStorageMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "nodeUrls"
					enclosedIn "params"
					desc ""
					location "body"
					type "List"
					optional false
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
            clz APIAddNodeToSurfsBackupStorageEvent.class
        }
    }
}