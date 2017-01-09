package org.zstack.storage.ceph.primary

org.zstack.header.storage.primary.APIQueryPrimaryStorageReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryCephPrimaryStorage"

    category "未知类别"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/primary-storage/ceph"

			url "GET /v1/primary-storage/ceph/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryCephPrimaryStorageMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryPrimaryStorageReply.class
        }
    }
}