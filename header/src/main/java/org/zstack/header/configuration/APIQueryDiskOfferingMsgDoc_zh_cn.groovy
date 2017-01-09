package org.zstack.header.configuration

org.zstack.header.configuration.APIQueryDiskOfferingReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryDiskOffering"

    category "configuration"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/disk-offerings"

			url "GET /v1/disk-offerings/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryDiskOfferingMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryDiskOfferingReply.class
        }
    }
}