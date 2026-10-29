package org.zstack.test.integration.networkservice.provider.virtualrouter.dns

import org.springframework.http.HttpEntity
import org.zstack.appliancevm.ApplianceVmGlobalConfig
import org.zstack.core.Platform
import org.zstack.core.rest.RESTFacadeImpl
import org.zstack.header.errorcode.SysErrors
import org.zstack.header.rest.BeforeAsyncJsonPostInterceptor
import org.zstack.header.rest.RESTConstant
import org.zstack.utils.FieldUtils
import java.util.concurrent.TimeUnit
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import org.zstack.core.db.Q
import org.zstack.network.service.virtualrouter.VirtualRouterCommands
import org.zstack.network.service.virtualrouter.VirtualRouterGlobalConfig
import org.zstack.sdk.ReconnectVirtualRouterAction
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.IpRangeSpec
import org.zstack.testlib.L3NetworkSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by shixin on 01/12/2018.
 */
class ReconnectVrApplyDnsServiceCase extends SubCase {
    EnvSpec env

    L3NetworkInventory l3
    String dns1 = "8.8.8.8"
    String dns2 = "8.8.4.4"
    String dns3 = "8.8.4.5"
    String dns4 = "8.8.4.6"

    @Override
    void clean() {
        env.cleanSimulatorHandlers();
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = VirtualRouterNetworkServiceEnv.oneVmOneHostVyosOnEipEnv()
        // Keep failure diagnostics on loopback, without connecting to a real router.
        env.find("pubL3", L3NetworkSpec).children.findAll { it instanceof IpRangeSpec }.each {
            it.startIp = "127.0.0.10"
            it.endIp = "127.0.0.100"
            it.gateway = "127.0.0.1"
        }
    }

    void testAddDns() {
        VirtualRouterCommands.SetDnsCmd cmd = null
        VmInstanceInventory vm = env.inventoryByName("vm")
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3")

        addDnsToL3Network {
            delegate.l3NetworkUuid = l3.getUuid()
            delegate.dns = dns1
        }
        addDnsToL3Network {
            delegate.l3NetworkUuid = l3.getUuid()
            delegate.dns = dns2
        }
        addDnsToL3Network {
            delegate.l3NetworkUuid = l3.getUuid()
            delegate.dns = dns3
        }
        /* public network dns will not be included in setdns */
        addDnsToL3Network {
            delegate.l3NetworkUuid = pubL3.getUuid()
            delegate.dns = dns4
        }

        env.afterSimulator(VirtualRouterConstant.VR_SET_DNS_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, VirtualRouterCommands.SetDnsCmd.class)
            return rsp
        }

        List<VirtualRouterVmVO> vrs = Q.New(VirtualRouterVmVO.class).list()

        reconnectVirtualRouter {
            vmInstanceUuid = vrs.get(0).uuid
        }

        assert cmd != null
        assert cmd.dns.size() == 3
        assert cmd.dns.get(0).dnsAddress == dns1
        assert cmd.dns.get(1).dnsAddress == dns2
        assert cmd.dns.get(2).dnsAddress == dns3
    }

    void testInitReconnectBudget() {
        String vrUuid = Q.New(VirtualRouterVmVO.class).list()[0].uuid
        String oldSshPort = VirtualRouterGlobalConfig.SSH_PORT.value()
        String oldTimeout = ApplianceVmGlobalConfig.CONNECT_TIMEOUT.value()
        RESTFacadeImpl rest = bean(RESTFacadeImpl.class)
        List<Long> timeouts = Collections.synchronizedList(new ArrayList<Long>())
        Runnable removeInterceptor = rest.installBeforeAsyncJsonPostInterceptor(new BeforeAsyncJsonPostInterceptor() {
            void beforeAsyncJsonPost(String url, Object body, TimeUnit unit, long timeout) {
                if (url.endsWith(VirtualRouterConstant.VR_INIT)) {
                    timeouts.add(unit.toMillis(timeout))
                }
            }
            void beforeAsyncJsonPost(String url, String body, TimeUnit unit, long timeout) {
                beforeAsyncJsonPost(url, (Object) body, unit, timeout)
            }
        })
        AtomicInteger calls = new AtomicInteger()
        try {
            // Failure diagnostics must not attempt SSH to the local test machine.
            updateGlobalConfig {
                category = VirtualRouterGlobalConfig.CATEGORY
                name = VirtualRouterGlobalConfig.SSH_PORT.name
                value = "1"
            }
            updateGlobalConfig {
                category = ApplianceVmGlobalConfig.CATEGORY
                name = ApplianceVmGlobalConfig.CONNECT_TIMEOUT.name
                value = "60"
            }
            env.afterSimulator(VirtualRouterConstant.VR_INIT) { rsp ->
                calls.incrementAndGet()
                // Set to 42000 for the customer timing reproduction; CI only needs a short callback delay.
                Thread.sleep(Long.getLong("vyos.init.slowCallbackMillis", 100L))
                return rsp
            }
            reconnectVirtualRouter {
                vmInstanceUuid = vrUuid
                timeout = 90_000
            }
            assert calls.get() == 1
            assert timeouts.size() == 1 && timeouts[0] > 42_000 && timeouts[0] <= 60_000

            calls.set(0)
            timeouts.clear()
            List<Long> arrivals = Collections.synchronizedList(new ArrayList<Long>())
            env.afterSimulator(VirtualRouterConstant.VR_INIT) { rsp, HttpEntity<String> e ->
                arrivals.add(System.nanoTime())
                if (calls.incrementAndGet() <= 2) {
                    // Inject the transport error at the REST boundary; an HTTP 500 is not a TCP reset.
                    def wrappers = FieldUtils.getFieldValue("wrappers", rest) as Map
                    def pending = wrappers[e.headers.getFirst(RESTConstant.TASK_UUID)]
                    assert pending != null
                    pending.fail(Platform.err(SysErrors.IO_ERROR, "Connection reset by peer"))
                }
                return rsp
            }
            reconnectVirtualRouter { vmInstanceUuid = vrUuid }
            assert calls.get() == 3 && timeouts.size() == 3
            assert timeouts[0] > timeouts[1] && timeouts[1] > timeouts[2]
            assert TimeUnit.NANOSECONDS.toMillis(arrivals[1] - arrivals[0]) >= 900
            assert TimeUnit.NANOSECONDS.toMillis(arrivals[2] - arrivals[1]) >= 1900

            calls.set(0)
            env.afterSimulator(VirtualRouterConstant.VR_INIT) { rsp ->
                calls.incrementAndGet()
                rsp.success = false
                rsp.error = "init configuration rejected"
                return rsp
            }
            assertInitReconnectFailure(vrUuid, "init configuration rejected")
            assert calls.get() == 1

            updateGlobalConfig {
                category = ApplianceVmGlobalConfig.CATEGORY
                name = ApplianceVmGlobalConfig.CONNECT_TIMEOUT.name
                value = "2"
            }
            calls.set(0)
            env.afterSimulator(VirtualRouterConstant.VR_INIT) { rsp, HttpEntity<String> e ->
                calls.incrementAndGet()
                def wrappers = FieldUtils.getFieldValue("wrappers", rest) as Map
                wrappers[e.headers.getFirst(RESTConstant.TASK_UUID)].fail(
                        Platform.err(SysErrors.IO_ERROR, "Connection refused"))
                return rsp
            }
            long start = System.nanoTime()
            assertInitReconnectFailure(vrUuid, "Connection refused")
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
            assert calls.get() == 2
            assert elapsed >= 1900 && elapsed < 5000

            calls.set(0)
            CountDownLatch finished = new CountDownLatch(1)
            env.afterSimulator(VirtualRouterConstant.VR_INIT) { rsp ->
                calls.incrementAndGet()
                try {
                    Thread.sleep(2500)
                    return rsp
                } finally {
                    finished.countDown()
                }
            }
            assertInitReconnectFailure(vrUuid, "Async Http Timeout")
            assert finished.await(5, TimeUnit.SECONDS)
            assert calls.get() == 1
        } finally {
            removeInterceptor.run()
            env.cleanSimulatorHandlers()
            updateGlobalConfig {
                category = VirtualRouterGlobalConfig.CATEGORY
                name = VirtualRouterGlobalConfig.SSH_PORT.name
                value = oldSshPort
            }
            updateGlobalConfig {
                category = ApplianceVmGlobalConfig.CATEGORY
                name = ApplianceVmGlobalConfig.CONNECT_TIMEOUT.name
                value = oldTimeout
            }
        }
    }

    private void assertInitReconnectFailure(String vrUuid, String expectedMessage) {
        def result = new ReconnectVirtualRouterAction(vmInstanceUuid: vrUuid,
                sessionId: env.session.uuid, timeout: 90_000).call()
        assert result.error != null
        assert !result.error.code.startsWith("sdk.")
        assert JSONObjectUtil.toJsonString(result.error).contains(expectedMessage)
    }

    @Override
    void test() {
        env.create {
            l3 = (env.specByName("l3") as L3NetworkSpec).inventory
            testAddDns()
            testInitReconnectBudget()
        }
    }
}
