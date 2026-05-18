package org.zstack.test.integration.kvm.host

import org.springframework.http.HttpEntity
import org.zstack.header.Constants
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.SysErrors
import org.zstack.header.rest.JsonAsyncRESTCallback
import org.zstack.header.rest.RESTFacade
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.HostInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class HostAgentRestartDrainCase extends SubCase {
    EnvSpec env
    HostInventory host
    RESTFacade restf

    static class StuckCall {
        CountDownLatch entered = new CountDownLatch(1)
        CountDownLatch release = new CountDownLatch(1)
        CountDownLatch done = new CountDownLatch(1)
        AtomicReference<ErrorCode> received = new AtomicReference<>()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.noVmEnv()
    }

    @Override
    void test() {
        env.create {
            host = env.inventoryByName("kvm")
            restf = bean(RESTFacade.class)
            testFirstConnectDrainsStaleCalls()
            testNoFirstConnectKeepsStaleCalls()
            testDifferentResourceUuidIsolated()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    private StuckCall sendStuckCall(String resourceUuid) {
        StuckCall h = new StuckCall()
        def stuckPath = "/test/stuck/${UUID.randomUUID()}"
        env.simulator(stuckPath) {
            h.entered.countDown()
            h.release.await(120, TimeUnit.SECONDS)
            return new KVMAgentCommands.AgentResponse()
        }

        Map<String, String> headers = [(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID): resourceUuid]
        restf.asyncJsonPost(restf.makeUrl(stuckPath), "{}", headers, new JsonAsyncRESTCallback<String>(null) {
            @Override void fail(ErrorCode err) { h.received.set(err); h.done.countDown() }
            @Override void success(String ret) { h.done.countDown() }
            @Override Class<String> getReturnClass() { return String.class }
        }, TimeUnit.MINUTES, 30)
        assert h.entered.await(5, TimeUnit.SECONDS)
        return h
    }

    private void overrideConnectResponse(boolean firstConnect, long agentStartTimeMillis) {
        env.afterSimulator(KVMConstant.KVM_CONNECT_PATH) { KVMAgentCommands.ConnectResponse rsp, HttpEntity<String> e ->
            rsp.firstConnect = firstConnect
            rsp.agentStartTimeMillis = agentStartTimeMillis
            return rsp
        }
    }

    void testFirstConnectDrainsStaleCalls() {
        StuckCall h = sendStuckCall(host.uuid)
        overrideConnectResponse(true, System.currentTimeMillis() + 70_000L)
        reconnectHost {
            uuid = host.uuid
        }
        assert h.done.await(5, TimeUnit.SECONDS)
        assert h.received.get() != null
        assert h.received.get().code == SysErrors.OPERATION_ERROR.toString()
        h.release.countDown()
    }

    void testNoFirstConnectKeepsStaleCalls() {
        StuckCall h = sendStuckCall(host.uuid)
        overrideConnectResponse(false, System.currentTimeMillis() + 70_000L)
        reconnectHost {
            uuid = host.uuid
        }
        assert !h.done.await(500, TimeUnit.MILLISECONDS)
        h.release.countDown()
    }

    void testDifferentResourceUuidIsolated() {
        StuckCall h = sendStuckCall("unrelated-resource-uuid")
        overrideConnectResponse(true, System.currentTimeMillis() + 70_000L)
        reconnectHost {
            uuid = host.uuid
        }
        assert !h.done.await(500, TimeUnit.MILLISECONDS)
        h.release.countDown()
    }
}
