package org.zstack.test.integration.kvm.host

import org.springframework.http.HttpEntity
import org.springframework.web.util.UriComponentsBuilder
import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.host.*
import org.zstack.header.rest.RESTConstant
import org.zstack.header.rest.RESTFacade
import org.zstack.header.vm.VmInstanceState
import org.zstack.kvm.*
import org.zstack.sdk.HostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

import static org.zstack.utils.CollectionDSL.e
import static org.zstack.utils.CollectionDSL.map

/**
 * Created by MaJin on 2017-05-01.
 */
class ReconnectHostCase extends SubCase {
    EnvSpec env
    HostInventory host
    RESTFacade restf
    static RECONNECT_TIME = 10

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            host = env.inventoryByName("kvm")
            restf = bean(RESTFacade.class)
            testReconnectHostVmState()
            testReconnectFailureHostVmState()
            testUpdateHostDuringConnecting()
            testChangeHostConnectionState()
            testInstallHostShutdownHookCmd()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testInstallHostShutdownHookCmd() {
        Boolean installHostShutdownHook = null
        env.afterSimulator(KVMConstant.KVM_CONNECT_PATH) { KVMAgentCommands.AgentResponse rsp, HttpEntity<String> e ->
            def connectCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.ConnectCmd.class)
            if (connectCmd.hostUuid.equals(host.uuid)) {
                installHostShutdownHook = connectCmd.installHostShutdownHook
            }
            return rsp
        }

        reconnectHost {
            uuid = host.uuid
        }

        assert !installHostShutdownHook

        KVMGlobalConfig.INSTALL_HOST_SHUTDOWN_HOOK.updateValue(true)

        reconnectHost {
            uuid = host.uuid
        }

        assert installHostShutdownHook

        KVMGlobalConfig.INSTALL_HOST_SHUTDOWN_HOOK.updateValue(KVMGlobalConfig.INSTALL_HOST_SHUTDOWN_HOOK.getDefaultValue())

        env.cleanAfterSimulatorHandlers()

        def isChangeStatus = false
        KVMAgentCommands.ReportHostStopEventCmd cmd = new KVMAgentCommands.ReportHostStopEventCmd()
        cmd.hostUuid = Platform.getUuid()

        env.message(ChangeHostStatusMsg.class) { ChangeHostStatusMsg msg, CloudBus bus ->
            isChangeStatus = true
            ChangeHostStatusReply r = new ChangeHostStatusReply()
            bus.reply(msg, r)
        }

        Map<String, String> header = map(e(RESTConstant.COMMAND_PATH, KVMConstant.KVM_REPORT_HOST_STOP_EVENT))
        UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(restf.getBaseUrl())
        ub.path(RESTConstant.COMMAND_CHANNEL_PATH)
        String url = ub.build().toUriString()
        restf.syncJsonPost(url, JSONObjectUtil.toJsonString(cmd), header, String.class)

        retryInSecs {
            assert isChangeStatus
        }

        isChangeStatus = false
        cmd.hostUuid = null
        restf.syncJsonPost(url, JSONObjectUtil.toJsonString(cmd), header, String.class)

        expect(AssertionError.class) {
            retryInSecs {
                assert isChangeStatus
            }
        }

        env.cleanMessageHandlers()
    }

    void testUpdateHostDuringConnecting() {
        boolean called
        env.simulator(KVMConstant.KVM_CONNECT_PATH) {
            def rsp = new KVMAgentCommands.ConnectResponse()
            rsp.success = true
            rsp.libvirtVersion = "1.0.0"
            rsp.qemuVersion = "1.3.0"
            updateKVMHost {
                uuid = host.uuid
                sshPort = 23
            }

            if (!called) {
                rsp.setError("on purpose")
            }

            return rsp
        }
        expect(AssertionError.class) {
            reconnectHost {
                uuid = host.uuid
            }
        }

        called = true
        assert Q.New(KVMHostVO.class).select(KVMHostVO_.port).findValue() == 23

        updateKVMHost {
            uuid = host.uuid
            sshPort = 22
        }
        assert Q.New(KVMHostVO.class).select(KVMHostVO_.port).findValue() == 22
        reconnectHost {
            uuid = host.uuid
        }

        assert Q.New(KVMHostVO.class).select(KVMHostVO_.port).findValue() == 23

        updateKVMHost {
            uuid = host.uuid
            sshPort = 22
        }
        env.cleanSimulatorHandlers()
    }

    void testReconnectFailureHostVmState() {
        VmInstanceInventory vmInv = env.inventoryByName("vm") as VmInstanceInventory

        env.simulator(KVMConstant.KVM_CONNECT_PATH) {
            throw new Exception("on purpose")
        }

        expect(AssertionError.class) {
            reconnectHost {
                uuid = vmInv.hostUuid
            }
        }

        retryInSecs {
            List<VmInstanceInventory> vmInvs = queryVmInstance {
                conditions = ["state=${VmInstanceState.Unknown}"]
            } as List<VmInstanceInventory>

            assert vmInvs.size() == 2
        }
    }

    // Reconnect host will not change VM's state, this test confirm VMs are always Running after host reconnecting
    void testReconnectHostVmState() {
        VmInstanceInventory vmInv = env.inventoryByName("vm") as VmInstanceInventory

        for (int i = 0; i < RECONNECT_TIME; i++) {
            reconnectHost {
                uuid = vmInv.hostUuid
                sessionId = currentEnvSpec.session.uuid
            }

            retryInSecs() {
                List<VmInstanceInventory> vmInvs = queryVmInstance {
                    conditions = ["state=${VmInstanceState.Running}"]
                } as List<VmInstanceInventory>

                assert vmInvs.size() == 2
            }
        }
    }

    void testChangeHostConnectionState() {
        HostInventory host = env.inventoryByName("kvm")

        // set host to disconnected
        SQL.New(HostVO.class)
                .eq(HostVO_.uuid, host.uuid)
                .set(HostVO_.status, HostStatus.Disconnected)
                .update()

        // extpEmitter.connectionReestablished()
        CloudBus bus = bean(CloudBus.class)
        ChangeHostConnectionStateMsg msg = new ChangeHostConnectionStateMsg()
        msg.hostUuid = host.uuid
        msg.connectionStateEvent = HostStatusEvent.connected.toString()
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.uuid)
        assert bus.call(msg).success
        assert Q.New(HostVO.class)
                .eq(HostVO_.uuid, host.uuid)
                .select(HostVO_.status)
                .findValue() == HostStatus.Connected
    }
}
