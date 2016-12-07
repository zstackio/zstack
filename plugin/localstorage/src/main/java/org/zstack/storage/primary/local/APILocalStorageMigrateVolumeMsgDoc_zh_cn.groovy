package org.zstack.storage.primary.local



doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "PUT /v1/primary-storage/local-storage/volumes/{volumeUuid}/actions"


            header (OAuth: 'the-session-uuid')

            clz APILocalStorageMigrateVolumeMsg.class

            desc ""
            
			params {

				column {
					name "volumeUuid"
					enclosedIn "localStorageMigrateVolume"
					desc "云盘UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "destHostUuid"
					enclosedIn "localStorageMigrateVolume"
					desc ""
					location "body"
					type "String"
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
            clz APILocalStorageMigrateVolumeEvent.class
        }
    }
}