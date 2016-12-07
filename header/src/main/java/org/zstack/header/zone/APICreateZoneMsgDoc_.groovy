package org.zstack.header.zone

doc {
    title "test"

    desc "this is a test"

    rest {
        request {
            url "POST /zones"
            header (
                    abc : "asdf",
                    xxx : 1
            )

            clz APICreateZoneMsg.class

            desc ""

            params {
                column {
                    name "name"
                    desc "zone name"
                    optional false
                    values ("xx","yyy","zzz")
                }
            }
        }

        response {
            body {

            }

            clz APICreateZoneEvent.class
        }
    }
}


