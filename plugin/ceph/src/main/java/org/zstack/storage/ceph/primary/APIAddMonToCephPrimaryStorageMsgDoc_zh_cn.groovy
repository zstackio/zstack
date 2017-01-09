package org.zstack.storage.ceph.primary

import org.zstack.storage.ceph.primary.APIAddMonToCephPrimaryStorageEvent

doc {
    title "AddMonToCephPrimaryStorage"

    category "未知类别"

    desc "在这里填写API描述"

    rest {
        request {
			url "POST /v1/primary-storage/ceph/{uuid}/mons"


            header (OAuth: 'the-session-uuid')

            clz APIAddMonToCephPrimaryStorageMsg.class

            desc ""
            
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
					name "monUrls"
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
            clz APIAddMonToCephPrimaryStorageEvent.class
        }
    }
}