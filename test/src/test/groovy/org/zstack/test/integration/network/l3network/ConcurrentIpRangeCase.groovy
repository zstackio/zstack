package org.zstack.test.integration.network.l3network

import org.zstack.sdk.*
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class ConcurrentIpRangeCase extends SubCase {
    EnvSpec env
    void setup() { useSpring(NetworkTest.springSpec) }
    void environment() {
        env = env { zone { name = 'zone' } }
    }
    void test() {
        env.create {
            verifyConcurrentRanges(false, 10, 19, 1)
            verifyConcurrentRanges(false, 15, 24, 1)
            verifyConcurrentRanges(false, 20, 29, 2)
            verifyConcurrentRanges(true, 10, 19, 1)
            verifyConcurrentCidr(false)
            verifyConcurrentCidr(true)
        }
    }
    private void verifyConcurrentRanges(boolean ipv6, int secondStart, int secondEnd, int successes) {
        def l2 = createL2NoVlanNetwork {
            name = "concurrent-${ipv6}-${secondStart}"; physicalInterface = 'eth0'
            zoneUuid = env.inventoryByName('zone').uuid
        }
        def l3 = createL3Network { name = 'concurrent-ranges'; l2NetworkUuid = l2.uuid }
        String session = adminSession()
        def actions = [[10, 19], [secondStart, secondEnd]].collect { bounds ->
            def action = ipv6 ? new AddIpv6RangeAction() : new AddIpRangeAction()
            action.name = 'concurrent-range'
            action.resourceUuid = org.zstack.core.Platform.getUuid()
            action.l3NetworkUuid = l3.uuid
            action.sessionId = session
            if (ipv6) {
                action.startIp = "2001:db8::${bounds[0]}"
                action.endIp = "2001:db8::${bounds[1]}"
                action.gateway = '2001:db8::1'; action.prefixLen = 64
                action.addressMode = 'Stateful-DHCP'
            } else {
                action.startIp = "192.168.230.${bounds[0]}"
                action.endIp = "192.168.230.${bounds[1]}"
                action.gateway = '192.168.230.1'; action.netmask = '255.255.255.0'
            }
            action
        }
        def results = runPair(actions)
        assert results.count { it.error == null } == successes
        results.findAll { it.error != null }.each { assert it.error.globalErrorCode == (ipv6 ? 'ORG_ZSTACK_NETWORK_L3_10037' : 'ORG_ZSTACK_NETWORK_L3_10060') }
        assert queryIpRange { conditions = ["l3NetworkUuid=${l3.uuid}"] }.size() == successes
        if (!ipv6 && secondStart != 15) {
            assert getIpAddressCapacity { l3NetworkUuids = [l3.uuid] }.totalCapacity == successes * 10
        }
        deleteL2Network { uuid = l2.uuid }
    }
    private void verifyConcurrentCidr(boolean ipv6) {
        def l2 = createL2NoVlanNetwork {
            name = 'concurrent-cidr'; physicalInterface = 'eth0'
            zoneUuid = env.inventoryByName('zone').uuid
        }
        def l3 = createL3Network { name = 'cidr'; l2NetworkUuid = l2.uuid }
        String session = adminSession()
        def actions = (1..2).collect {
            def action = ipv6 ? new AddIpv6RangeByNetworkCidrAction() : new AddIpRangeByNetworkCidrAction()
            action.name = 'concurrent-cidr'; action.resourceUuid = org.zstack.core.Platform.getUuid()
            action.l3NetworkUuid = l3.uuid; action.sessionId = session
            action.networkCidr = ipv6 ? '2001:db8::/64' : '192.168.230.0/24'
            if (ipv6) { action.addressMode = 'Stateful-DHCP' }
            action
        }
        def results = runPair(actions)
        assert results.count { it.error == null } == 1
        assert results.find { it.error != null }.error.globalErrorCode == (ipv6 ? 'ORG_ZSTACK_NETWORK_L3_10037' : 'ORG_ZSTACK_NETWORK_L3_10060')
        assert queryIpRange { conditions = ["l3NetworkUuid=${l3.uuid}"] }.size() == 1
        deleteL2Network { uuid = l2.uuid }
    }

    private List runPair(List actions) {
        def ready = new java.util.concurrent.CountDownLatch(2)
        def active = new java.util.concurrent.atomic.AtomicBoolean(true)
        Set ids = actions.collect { it.resourceUuid } as Set
        bean(org.zstack.core.cloudbus.CloudBus).installBeforeDeliveryMessageInterceptor(
                new org.zstack.header.message.BeforeDeliveryMessageInterceptor() {
            int orderOfBeforeDeliveryMessageInterceptor() { 0 }
            void beforeDeliveryMessage(org.zstack.header.message.Message msg) {
                if (active.get() && msg.serviceId.endsWith(':::network.l3') && ids.contains(msg.resourceUuid)) {
                    ready.countDown()
                    assert ready.await(30, java.util.concurrent.TimeUnit.SECONDS)
                }
            }
        }, org.zstack.header.network.l3.APIAddIpRangeMsg, org.zstack.header.network.l3.APIAddIpv6RangeMsg,
                org.zstack.header.network.l3.APIAddIpRangeByNetworkCidrMsg,
                org.zstack.header.network.l3.APIAddIpv6RangeByNetworkCidrMsg)
        List results = Collections.synchronizedList([])
        List<Throwable> failures = Collections.synchronizedList([])
        List<Thread> threads = actions.collect { action ->
            Thread.start {
                try {
                    results.add(action.call())
                } catch (Throwable error) {
                    failures.add(error)
                }
            }
        }
        try {
            threads.each { it.join(60000) }
            assert threads.every { !it.alive }: 'concurrent API requests must complete'
            if (!failures.isEmpty()) {
                throw failures.first()
            }
            assert results.size() == actions.size()
            assert ready.count == 0: 'both requests must pass API validation before either is delivered to L3'
            return results
        } finally {
            active.set(false)
            threads.findAll { it.alive }.each { it.interrupt() }
        }
    }

    void clean() { env.delete() }
}
