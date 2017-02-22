package org.zstack.test.integration.l3network.getfreeip

import org.zstack.testlib.Test

/**
 * Created by xing5 on 2017/2/21.
 */
class GetFreeIpTest extends Test {
    def DOC = """

    Test getting free IPs from a single L3 network with one or two IP ranges
    
"""
    @Override
    void setup() {
        spring {
            include("vip.xml")
        }
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        runSubCases([
                new OneL3OneIpRangeNoIpUsed(),
                new OneL3OneIpRangeSomeIpUsed(),
                new OneL3TwoIpRanges()
        ])
    }
}
