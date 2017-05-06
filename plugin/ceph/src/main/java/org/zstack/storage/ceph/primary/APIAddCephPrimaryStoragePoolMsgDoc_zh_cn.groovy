package org.zstack.storage.ceph.primary

import org.zstack.storage.ceph.primary.APIAddCephPrimaryStoragePoolEvent

doc {
    title "AddCephPrimaryStoragePool"

    category "未知类别"

    desc """在这里填写API描述"""

    rest {
        request {
			url "POST /v1/primary-storage/ceph/{primaryStorageUuid}/pools"

			header (Authorization: 'OAuth the-session-uuid')


            clz APIAddCephPrimaryStoragePoolMsg.class

            desc """"""
            
			params {

				column {
					name "primaryStorageUuid"
					enclosedIn ""
					desc "主存储UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "name"
					enclosedIn ""
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "description"
					enclosedIn ""
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "errorIfNotExist"
					enclosedIn ""
					desc ""
					location "body"
					type "boolean"
					optional true
					since "0.6"
					
				}
				column {
					name "resourceUuid"
					enclosedIn ""
					desc ""
					location "body"
					type "String"
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
            clz APIAddCephPrimaryStoragePoolEvent.class
        }
    }
}