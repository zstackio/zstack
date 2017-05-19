package org.zstack.storage.fusionstor.backup

import org.zstack.storage.fusionstor.backup.APIRemoveMonFromFusionstorBackupStorageEvent

doc {
    title "RemoveMonFromFusionstorBackupStorage"

    category "未知类别"

    desc """在这里填写API描述"""

    rest {
        request {
			url "DELETE /v1/backup-storage/fusionstor/{uuid}/mons"

			header (Authorization: 'OAuth the-session-uuid')


            clz APIRemoveMonFromFusionstorBackupStorageMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "monHostnames"
					enclosedIn ""
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
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIRemoveMonFromFusionstorBackupStorageEvent.class
        }
    }
}