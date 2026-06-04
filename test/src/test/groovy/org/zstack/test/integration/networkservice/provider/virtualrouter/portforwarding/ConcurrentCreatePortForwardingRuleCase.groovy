package org.zstack.test.integration.networkservice.provider.virtualrouter.portforwarding

import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_MEDIATOR_10007
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_NETWORK_SERVICE_PORTFORWARDING_10017

import org.zstack.core.db.Q
import org.zstack.network.service.portforwarding.PortForwardingProtocolType
import org.zstack.network.service.portforwarding.PortForwardingRuleVO
import org.zstack.network.service.portforwarding.PortForwardingRuleVO_
import org.zstack.sdk.CreatePortForwardingRuleAction
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VipInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ConcurrentCreatePortForwardingRuleCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            zone {
                name = "zone"

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "pubL3"

                        ip {
                            startIp = "11.168.100.10"
                            endIp = "11.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.100.1"
                        }
                    }
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            testConcurrentCreateSamePortForwardingRule()
        }
    }

    void testConcurrentCreateSamePortForwardingRule() {
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3") as L3NetworkInventory
        VipInventory vip = createVip {
            name = "vip"
            l3NetworkUuid = pubL3.uuid
        } as VipInventory

        int requestCount = 50
        String sessionId = adminSession()
        Closure<CreatePortForwardingRuleAction.Result> createRule = { String name, int vipPortStart, int vipPortEnd ->
            CreatePortForwardingRuleAction action = new CreatePortForwardingRuleAction()
            action.name = name
            action.vipUuid = vip.uuid
            action.vipPortStart = vipPortStart
            action.vipPortEnd = vipPortEnd
            action.privatePortStart = vipPortStart
            action.privatePortEnd = vipPortEnd
            action.protocolType = PortForwardingProtocolType.TCP.toString()
            action.sessionId = sessionId
            return action.call()
        }
        CountDownLatch ready = new CountDownLatch(requestCount)
        CountDownLatch start = new CountDownLatch(1)
        CountDownLatch done = new CountDownLatch(requestCount)
        def results = new ConcurrentLinkedQueue<CreatePortForwardingRuleAction.Result>()
        def errors = new ConcurrentLinkedQueue<Throwable>()

        (0..<requestCount).each { int index ->
            Thread.start("create-same-port-forwarding-rule-${index}") {
                try {
                    ready.countDown()
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new AssertionError("not all createPortForwardingRule workers became ready within 10 seconds")
                    }

                    results.add(createRule("pf-${index}", 8080, 8080))
                } catch (Throwable t) {
                    errors.add(t)
                } finally {
                    done.countDown()
                }
            }
        }

        assert ready.await(10, TimeUnit.SECONDS) :
                "createPortForwardingRule workers did not all become ready: expected=${requestCount}"
        start.countDown()
        assert done.await(60, TimeUnit.SECONDS) :
                "concurrent createPortForwardingRule requests did not finish within 60 seconds"
        assert errors.isEmpty() :
                "worker thread threw unexpected errors: ${errors.collect { it.message }.join('; ')}"

        def successes = results.findAll { it.error == null }
        def failures = results.findAll { it.error != null }
        assert successes.size() == 1 :
                "same VIP/protocol/port create must succeed once: expectedSuccess=1, actualSuccess=${successes.size()}, results=${results.size()}"
        assert failures.size() == requestCount - 1 :
                "duplicate same VIP/protocol/port creates must fail: expectedFailures=${requestCount - 1}, actualFailures=${failures.size()}"
        Set<String> conflictErrorCodes = [
                ORG_ZSTACK_NETWORK_SERVICE_PORTFORWARDING_10017,
                ORG_ZSTACK_MEDIATOR_10007
        ] as Set
        assert failures.every { conflictErrorCodes.contains(it.error.globalErrorCode) } :
                "duplicate failures should be VIP port conflict errors: errors=${failures.collect { it.error.globalErrorCode }}"

        def lowerBoundaryConflict = createRule("pf-lower-boundary-conflict", 8079, 8080)
        assert lowerBoundaryConflict.error != null :
                "closed VIP port range should conflict when new end touches existing start"
        assert conflictErrorCodes.contains(lowerBoundaryConflict.error.globalErrorCode) :
                "lower boundary conflict should report VIP port conflict error: error=${lowerBoundaryConflict.error}"

        def upperBoundaryConflict = createRule("pf-upper-boundary-conflict", 8080, 8081)
        assert upperBoundaryConflict.error != null :
                "closed VIP port range should conflict when new start touches existing end"
        assert conflictErrorCodes.contains(upperBoundaryConflict.error.globalErrorCode) :
                "upper boundary conflict should report VIP port conflict error: error=${upperBoundaryConflict.error}"

        def lowerAdjacent = createRule("pf-lower-adjacent", 8079, 8079)
        assert lowerAdjacent.error == null :
                "adjacent lower VIP port should not conflict with existing port 8080: error=${lowerAdjacent.error}"

        def upperAdjacent = createRule("pf-upper-adjacent", 8081, 8081)
        assert upperAdjacent.error == null :
                "adjacent upper VIP port should not conflict with existing port 8080: error=${upperAdjacent.error}"

        long sameRules = Q.New(PortForwardingRuleVO.class)
                .eq(PortForwardingRuleVO_.vipUuid, vip.uuid)
                .eq(PortForwardingRuleVO_.protocolType, PortForwardingProtocolType.TCP)
                .eq(PortForwardingRuleVO_.vipPortStart, 8080)
                .eq(PortForwardingRuleVO_.vipPortEnd, 8080)
                .eq(PortForwardingRuleVO_.privatePortStart, 8080)
                .eq(PortForwardingRuleVO_.privatePortEnd, 8080)
                .count()
        assert sameRules == 1L :
                "database must not contain duplicate same VIP/protocol/port rules: expected=1, actual=${sameRules}, vipUuid=${vip.uuid}"
    }

    @Override
    void clean() {
        env.delete()
    }
}
