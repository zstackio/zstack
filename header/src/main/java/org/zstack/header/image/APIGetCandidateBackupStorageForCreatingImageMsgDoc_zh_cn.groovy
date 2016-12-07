package org.zstack.header.image



doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/images/volumes/{volumeUuid}/candidate-backup-storage"

			url "GET /v1/images/volume-snapshots/{volumeSnapshotUuid}/candidate-backup-storage"


            header (OAuth: 'the-session-uuid')

            clz APIGetCandidateBackupStorageForCreatingImageMsg.class

            desc ""
            
			params {

				column {
					name "volumeUuid"
					enclosedIn ""
					desc "云盘UUID"
					location "query"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "volumeSnapshotUuid"
					enclosedIn ""
					desc "云盘快照UUID"
					location "query"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIGetCandidateBackupStorageForCreatingImageReply.class
        }
    }
}