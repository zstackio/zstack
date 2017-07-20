package org.zstack.test.unittest.org.zstack.test.unittest.utils

import org.junit.Test
import org.zstack.utils.network.NetworkUtils
/**
 * Created by mingjian.deng on 2017/7/20.
 */
class NetworkUtilsCase {
    static List<String> dstCidr = Arrays.asList("10.75.0.1/32", "10.75.0.2/31",
            "10.75.0.4/30", "10.75.0.8/29", "10.75.0.16/28", "10.75.0.32/27", "10.75.0.64/26", "10.75.0.128/25").sort()

    @Test
    void testIpRangeToCidr() {
        List<String> cidrs = NetworkUtils.getCidrsFromIpRange("10.75.0.1", "10.75.0.255", false)
        assert cidrs.size() == 1
        assert cidrs.get(0) == "10.75.0.0/24"

        cidrs = NetworkUtils.getCidrsFromIpRange("10.75.0.1", "10.75.0.255")
        assert cidrs.size() == dstCidr.size()
        assert cidrs.sort().toString() == dstCidr.toString()
    }
}
