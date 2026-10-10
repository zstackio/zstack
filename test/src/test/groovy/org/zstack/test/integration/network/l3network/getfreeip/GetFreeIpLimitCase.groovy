package org.zstack.test.integration.network.l3network.getfreeip

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Filter
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.core.filter.AbstractFilter
import org.zstack.sdk.IpRangeInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.network.IPv6Constants

/**
 * Small ranges retain existing query behavior; negative and excessive positive limits return at most 65535 IPs.
 * Multiple ranges share the same limit without repeatedly deducting accumulated results.
 */
class GetFreeIpLimitCase extends SubCase {
    EnvSpec env
    String l2Uuid

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            zone {
                name = "zone"
                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"
                }
            }
        }
    }

    @Override
    void test() {
        env.create {}
        l2Uuid = env.inventoryByName("l2").uuid
        testSingleRangesRespectLimit()
        testMultipleRangesShareLimit()
    }

    @Override
    void clean() {
        env.delete()
    }

    void testSingleRangesRespectLimit() {
        L3NetworkInventory l3 = createL3Network {
            name = "single-range-queries"
            l2NetworkUuid = l2Uuid
        }
        [
                [version: 4, gateway: "10.6.0.1", free: 10, limits: [65534, 65535, 65536],
                 small: ["10.6.0.10", "10.6.0.20"], large: ["10.6.1.10", "10.7.1.9"]],
                [version: 6, gateway: "2001:db8:4::1", free: 101, limits: [-1, Integer.MAX_VALUE],
                 small: ["2001:db8:4::10", "2001:db8:4::75"], large: ["2001:db8:4::100", "2001:db8:4:0:ffff:ffff:ffff:ffff"]]
        ].each { data ->
            List<IpRangeInventory> ranges = [data.small, data.large].collect { bounds ->
                if (data.version == 4) {
                    return addIpRange {
                        name = "ipv4-${bounds[0]}"
                        l3NetworkUuid = l3.uuid
                        startIp = bounds[0]
                        endIp = bounds[1]
                        gateway = data.gateway
                        netmask = "255.254.0.0"
                    }
                }
                return addIpv6Range {
                    name = "ipv6-${bounds[0]}"
                    l3NetworkUuid = l3.uuid
                    startIp = bounds[0]
                    endIp = bounds[1]
                    gateway = data.gateway
                    prefixLen = 64
                    addressMode = IPv6Constants.Stateful_DHCP
                }
            }
            IpRangeInventory small = ranges[0]
            def freeIps = getFreeIpOfIpRange {
                ipRangeUuid = small.uuid
                limit = 0
            }
            assert freeIps*.ip == [small.startIp] : "legacy zero-limit IP: expected=${small.startIp}, actual=${freeIps*.ip}"
            def vip = createVip {
                name = "occupied-first-ipv${data.version}"
                l3NetworkUuid = l3.uuid
                ipVersion = data.version
                requiredIp = small.startIp
            }
            freeIps = getFreeIpOfIpRange {
                ipRangeUuid = small.uuid
                limit = 0
            }
            int zeroCount = data.version == 4 ? 0 : 1
            assert freeIps.size() == zeroCount : "occupied zero-limit count: expected=${zeroCount}, actual=${freeIps.size()}"
            assert freeIps.every { it.ip != vip.ip } : "zero-limit query must exclude occupied IP: ip=${vip.ip}"
            freeIps = getFreeIpOfIpRange {
                ipRangeUuid = small.uuid
            }
            int defaultCount = data.version == 4 ? 10 : 100
            assert freeIps.size() == defaultCount : "default count: expected=${defaultCount}, actual=${freeIps.size()}"
            [-1, -2, 2].each { requestedLimit ->
                freeIps = getFreeIpOfIpRange {
                    ipRangeUuid = small.uuid
                    limit = requestedLimit
                }
                int expected = requestedLimit == 2 ? 2 : data.free
                assert freeIps.size() == expected : "small-range count: expected=${expected}, actual=${freeIps.size()}, limit=${requestedLimit}"
                assert freeIps.every { it.ip != vip.ip } : "occupied IP must be excluded: ip=${vip.ip}"
            }
            // Cover shared limit boundaries once to avoid redundant large replies across 100 stability runs.
            String responsePath = "/${ranges[1].uuid}/ip/free"
            Logger apiLogger = (Logger) LogManager.getLogger("api.request")
            AbstractFilter responseFilter = new AbstractFilter() {
                @Override
                Filter.Result filter(LogEvent event) {
                    String message = event.message.formattedMessage
                    return message.contains(responsePath) && message.contains("Response to") &&
                            message.contains("Status Code: 200,") ? Filter.Result.DENY : Filter.Result.NEUTRAL
                }
            }
            // Restrict suppression to this range's successful responses and always remove the filter afterward.
            apiLogger.addFilter(responseFilter)
            try {
                data.limits.each { requestedLimit ->
                    freeIps = getFreeIpOfIpRange {
                        ipRangeUuid = ranges[1].uuid
                        limit = requestedLimit
                    }
                    logger.info("free IP response: version=${data.version}, limit=${requestedLimit}, count=${freeIps.size()}")
                    int expected = requestedLimit > 0 && requestedLimit < 65535 ? requestedLimit : 65535
                    assert freeIps.size() == expected : "large-range count: expected=${expected}, actual=${freeIps.size()}, version=${data.version}, limit=${requestedLimit}"
                }
            } finally {
                apiLogger.get().removeFilter(responseFilter)
            }
        }
    }

    void testMultipleRangesShareLimit() {
        L3NetworkInventory l3 = createL3Network {
            name = "multiple-ipv6-ranges"
            l2NetworkUuid = l2Uuid
            ipVersion = 6
        }
        [
                [start: "2001:db8:5::10", end: "2001:db8:5::12"],
                [start: "2001:db8:5::20", end: "2001:db8:5::22"],
                [start: "2001:db8:5::30", end: "2001:db8:5::32", expected: 9],
                [start: "2001:db8:5::40", end: "2001:db8:5::41", expected: 10]
        ].eachWithIndex { range, index ->
            addIpv6Range {
                name = "multiple-range-${index}"
                l3NetworkUuid = l3.uuid
                startIp = range.start
                endIp = range.end
                gateway = "2001:db8:5::1"
                prefixLen = 64
                addressMode = IPv6Constants.Stateful_DHCP
            }
            if (range.expected != null) {
                def freeIps = getFreeIpOfL3Network {
                    l3NetworkUuid = l3.uuid
                    limit = 10
                }
                assert freeIps.size() == range.expected : "multiple-range count: expected=${range.expected}, actual=${freeIps.size()}"
            }
        }
    }
}
