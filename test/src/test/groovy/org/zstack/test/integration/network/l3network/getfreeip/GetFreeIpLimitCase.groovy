package org.zstack.test.integration.network.l3network.getfreeip

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Filter
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.core.filter.AbstractFilter
import org.zstack.header.network.l3.IpRangeVO
import org.zstack.header.network.l3.ReservedIpRangeVO
import org.zstack.network.l3.IpRangeHelper
import org.zstack.sdk.GetFreeIpOfIpRangeAction
import org.zstack.sdk.GetFreeIpOfL3NetworkAction
import org.zstack.sdk.GetFreeIpResult
import org.zstack.sdk.IpRangeInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.network.IPv6Constants

/**
 * Return at most 1000 available IPs and report whether another available IP exists.
 * Preserve small-range and start behavior; zero limits return no IPs and report availability.
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
                [version: 4, gateway: "10.6.0.1", free: 10, limits: [999, 1000, 1001],
                 small: ["10.6.0.10", "10.6.0.20"], large: ["10.6.1.10", "10.6.4.242"],
                 split: [["10.6.0.12", "10.6.0.14"], ["10.6.0.13", "10.6.0.15"], ["10.6.0.17", "10.6.0.18"]],
                 middle: "10.6.0.16", tail: ["10.6.4.239", "10.6.4.240", "10.6.4.241", "10.6.4.242"],
                 start: "10.6.0.12", first: "10.6.0.12", reserved: ["10.6.0.11", "10.6.0.18"], available: ["10.6.0.19", "10.6.0.20"]],
                [version: 6, gateway: "2001:db8:4::1", free: 101, limits: [-1, Integer.MAX_VALUE],
                 small: ["2001:db8:4::10", "2001:db8:4::75"], large: ["2001:db8:4::100", "2001:db8:4:0:ffff:ffff:ffff:ffff"],
                 split: [["2001:db8:4::12", "2001:db8:4::14"], ["2001:db8:4::13", "2001:db8:4::15"], ["2001:db8:4::17", "2001:db8:4::73"]],
                 middle: "2001:db8:4::16", tail: ["2001:db8:4:0:ffff:ffff:ffff:fffc", "2001:db8:4:0:ffff:ffff:ffff:fffd", "2001:db8:4:0:ffff:ffff:ffff:fffe", "2001:db8:4:0:ffff:ffff:ffff:ffff"],
                 start: "2001:db8:4::12", first: "2001:db8:4::13", reserved: ["2001:db8:4::11", "2001:db8:4::73"], available: ["2001:db8:4::74", "2001:db8:4::75"]]
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
            def result = queryRange(small.uuid, 0)
            assertReply(result, 0, true)
            def vip = createVip {
                name = "occupied-first-ipv${data.version}"
                l3NetworkUuid = l3.uuid
                ipVersion = data.version
                requiredIp = small.startIp
            }
            assertReply(queryRange(small.uuid, 0), 0, true)
            assertReply(queryRange(small.uuid), data.version == 4 ? 10 : 100, data.free > 100)
            [-1, -2, 2].each { requestedLimit ->
                result = queryRange(small.uuid, requestedLimit)
                assertReply(result, requestedLimit == 2 ? 2 : data.free, requestedLimit == 2)
                assert result.inventories.every { it.ip != vip.ip } : "occupied IP must be excluded: ip=${vip.ip}"
            }
            result = queryRange(small.uuid, 2, data.start)
            assertReply(result, 2, true)
            assert result.inventories[0].ip == data.first : "start behavior: expected=${data.first}, actual=${result.inventories[0].ip}"

            // The API rejects overlapping reservations; exercise subtraction without persisting synthetic ranges.
            IpRangeVO smallRange = dbFindByUuid(small.uuid, IpRangeVO.class)
            def reservedSegments = data.split.collect { bounds ->
                new ReservedIpRangeVO(ipVersion: data.version, startIp: bounds[0], endIp: bounds[1])
            }
            String queryStart = data.version == 4 ? "0.0.0.0" : "::"
            assert IpRangeHelper.getFreeIp(smallRange, 3, queryStart, reservedSegments)*.ip == [data.reserved[0], data.middle, data.available[0]] : "remaining segments must share the limit"
            assert IpRangeHelper.getFreeIp(smallRange, 2, data.split[0][1], reservedSegments)*.ip == [data.middle, data.available[0]] : "start inside a reservation must reach the next segment"
            assert IpRangeHelper.getFreeIp(smallRange, 3, data.middle, reservedSegments)*.ip == (data.version == 4 ? [data.middle] + data.available : data.available) : "segment start must retain IPv4 inclusive and IPv6 exclusive behavior"
            // Adjacent reservations leave open intervals without an integer address.
            reservedSegments.add(new ReservedIpRangeVO(ipVersion: data.version, startIp: data.middle, endIp: data.middle))
            assert IpRangeHelper.getFreeIp(smallRange, 3, queryStart, reservedSegments)*.ip == [data.reserved[0]] + data.available : "adjacent reservations must leave only available integer addresses"

            addReservedIpRange {
                l3NetworkUuid = l3.uuid
                startIp = data.reserved[0]
                endIp = data.reserved[1]
            }
            result = queryRange(small.uuid, 2)
            assertReply(result, 2, false)
            assert result.inventories*.ip == data.available : "available IPs after filtering: expected=${data.available}, actual=${result.inventories*.ip}"
            assertReply(queryRange(small.uuid, 1), 1, true)
            assertReply(queryRange(small.uuid, 0, data.available[1]), 0, data.version == 4)
            addReservedIpRange {
                l3NetworkUuid = l3.uuid
                startIp = data.available[0]
                endIp = data.available[1]
            }
            assertReply(queryRange(small.uuid, 0), 0, false)
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
                    result = queryRange(ranges[1].uuid, requestedLimit)
                    assertReply(result, requestedLimit > 0 && requestedLimit < 1000 ? requestedLimit : 1000, true)
                }
                if (data.version == 4) {
                    assertReply(queryRange(ranges[1].uuid, 1000, "10.6.1.11"), 1000, false)
                    assertReply(queryRange(ranges[1].uuid, 1000, "10.6.1.12"), 999, false)
                }

                // Large reservations cannot be created through the API without enumerating them first.
                IpRangeVO largeRange = dbFindByUuid(ranges[1].uuid, IpRangeVO.class)
                def largeReserved = [new ReservedIpRangeVO(ipVersion: data.version, startIp: largeRange.startIp, endIp: data.tail[1])]
                assert IpRangeHelper.getFreeIp(largeRange, 2, queryStart, largeReserved)*.ip == data.tail[2..3] : "large reservations must be skipped"
                assert IpRangeHelper.getFreeIp(largeRange, 2, data.tail[2], largeReserved)*.ip == (data.version == 4 ? data.tail[2..3] : [data.tail[3]]) : "large start offsets must preserve existing behavior"
                largeReserved.add(new ReservedIpRangeVO(ipVersion: data.version, startIp: data.tail[0], endIp: data.tail[2]))
                assert IpRangeHelper.getFreeIp(largeRange, 2, queryStart, largeReserved)*.ip == [data.tail[3]] : "overlapping reservations must leave only the final IP"
                largeReserved.add(new ReservedIpRangeVO(ipVersion: data.version, startIp: largeRange.startIp, endIp: largeRange.endIp))
                assert IpRangeHelper.getFreeIp(largeRange, 2, queryStart, largeReserved).isEmpty() : "fully reserved ranges must return no IPs"
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
                [start: "2001:db8:5::40", end: "2001:db8:5::40", expected: 10],
                [start: "2001:db8:5::50", end: "2001:db8:5::50", expected: 10, more: true]
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
                assertReply(queryNetwork(l3.uuid, 10), range.expected, range.more == true)
            }
        }
        [
                ["2001:db8:5::10", "2001:db8:5::12"],
                ["2001:db8:5::20", "2001:db8:5::20"],
                ["2001:db8:5::22", "2001:db8:5::22"],
                ["2001:db8:5::31", "2001:db8:5::31"]
        ].each { bounds ->
            addReservedIpRange {
                l3NetworkUuid = l3.uuid
                startIp = bounds[0]
                endIp = bounds[1]
            }
        }
        Set<String> availableIps = ["2001:db8:5::21", "2001:db8:5::30", "2001:db8:5::32", "2001:db8:5::40", "2001:db8:5::50"] as Set
        def result = queryNetwork(l3.uuid, 5)
        assertReply(result, 5, false)
        assert (result.inventories*.ip).toSet() == availableIps : "multiple ranges must exclude all reserved IPs"
        result = queryNetwork(l3.uuid, 4)
        assertReply(result, 4, true)
        Set<String> returnedIps = (result.inventories*.ip).toSet()
        assert returnedIps.size() == 4 && availableIps.containsAll(returnedIps) : "truncated results must contain distinct available IPs"
        assertReply(queryNetwork(l3.uuid, 0), 0, true)
        availableIps.each { ip ->
            addReservedIpRange {
                l3NetworkUuid = l3.uuid
                startIp = ip
                endIp = ip
            }
        }
        assertReply(queryNetwork(l3.uuid, 0), 0, false)
    }

    GetFreeIpResult queryRange(String uuid, Integer requestedLimit = null, String startIp = null) {
        GetFreeIpOfIpRangeAction action = new GetFreeIpOfIpRangeAction()
        action.ipRangeUuid = uuid
        action.start = startIp
        action.sessionId = adminSession()
        if (requestedLimit != null) {
            action.limit = requestedLimit
        }
        def result = action.call().throwExceptionIfError().value
        logger.info("free IP response: range=${uuid}, limit=${action.limit}, count=${result.inventories.size()}, more=${result.more}")
        return result
    }

    GetFreeIpResult queryNetwork(String uuid, int requestedLimit) {
        GetFreeIpOfL3NetworkAction action = new GetFreeIpOfL3NetworkAction()
        action.l3NetworkUuid = uuid
        action.limit = requestedLimit
        action.sessionId = adminSession()
        return action.call().throwExceptionIfError().value
    }

    void assertReply(GetFreeIpResult result, int count, boolean more) {
        assert result.inventories.size() == count : "free IP count: expected=${count}, actual=${result.inventories.size()}"
        assert result.more == more : "more flag: expected=${more}, actual=${result.more}"
    }

}
