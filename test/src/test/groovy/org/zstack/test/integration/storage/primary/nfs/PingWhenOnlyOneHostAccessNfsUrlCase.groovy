package org.zstack.test.integration.storage.primary.nfs

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.Q
import org.zstack.header.Constants
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.header.message.MessageReply
import org.zstack.header.storage.primary.PingPrimaryStorageMsg
import org.zstack.header.storage.primary.PrimaryStorageConstant
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO_
import org.zstack.header.storage.primary.PrimaryStorageHostStatus
import org.zstack.header.storage.primary.PrimaryStorageStatus
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.storage.primary.PrimaryStorageVO_
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackend
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands
import org.zstack.test.integration.storage.NfsEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.Utils
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.logging.CLogger

/**
 * Created by MaJin on 2017-05-07.
 */
class PingWhenOnlyOneHostAccessNfsUrlCase extends SubCase{

    private final static CLogger logger = Utils.getLogger(PingWhenOnlyOneHostAccessNfsUrlCase.class)
    EnvSpec env
    HostInventory host
    PrimaryStorageInventory ps1, ps2
    CloudBus bus
    static int retryTimes = 8

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = NfsEnv.TwoNfsTwoHostOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            host = env.inventoryByName("kvm") as HostInventory
            ps1 = env.inventoryByName("nfs1") as PrimaryStorageInventory
            ps2 = env.inventoryByName("nfs2") as PrimaryStorageInventory
            bus = bean(CloudBus.class)
            // follow 3 tests is host1 and host2 ping ps1
            testNotAllHostPingNfsSuccess()
            testAllHostPingNfsFail()
            testAllHostPingNfsSucess()

            // follow test is host1 ping ps1 and ps2
            testHostPingAllNfsFail()
        }

    }

    @Override
    void clean() {
        env.delete()
    }

    void testNotAllHostPingNfsSuccess(){
        List<Boolean> rets = Collections.synchronizedList(new ArrayList<Boolean>())

        def checked1 = false
        env.simulator(NfsPrimaryStorageKVMBackend.PING_PATH) { HttpEntity<String> e, EnvSpec espec ->
            checked1 = true
            def cmd = JSONObjectUtil.toObject(e.getBody(), NfsPrimaryStorageKVMBackendCommands.PingCmd.class)
            def huuid = e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)
            def rsp = new NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse()
            if (huuid == host.getUuid() && cmd.uuid == ps1.uuid){
                rsp.setError("Connection error1")
                rsp.setSuccess(false)
                rets.add(false)
            }else {
                rets.add(true)
            }
            return rsp
        }

        PingPrimaryStorageMsg msg = new PingPrimaryStorageMsg()
        msg.setPrimaryStorageUuid(ps1.getUuid())
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, ps1.uuid)
        MessageReply reply = bus.call(msg)
        assert checked1
        assert reply.success
        assert (rets.get(0) && rets.size() == 1) || (!rets.get(0) && rets.size() == 2)
        if(!rets.get(0)){
            assert Q.New(PrimaryStorageHostRefVO.class)
                    .eq(PrimaryStorageHostRefVO_.hostUuid, host.getUuid())
                    .eq(PrimaryStorageHostRefVO_.primaryStorageUuid, ps1.uuid)
                    .select(PrimaryStorageHostRefVO_.status)
                    .findValue() == PrimaryStorageHostStatus.Disconnected
            assert Q.New(HostVO.class)
                    .eq(HostVO_.uuid, host.getUuid())
                    .select(HostVO_.status)
                    .findValue() == HostStatus.Connected
        }

    }

    void testAllHostPingNfsFail(){
        def checked2 = false
        env.simulator(NfsPrimaryStorageKVMBackend.PING_PATH) { HttpEntity<String> e, EnvSpec espec ->
            checked2 = true
            def rsp = new NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse()
            def cmd = JSONObjectUtil.toObject(e.getBody(), NfsPrimaryStorageKVMBackendCommands.PingCmd.class)
            if (cmd.uuid == ps1.uuid) {
                rsp.setError("Connection error2")
                rsp.setSuccess(false)
            }
            return rsp
        }

        PingPrimaryStorageMsg msg = new PingPrimaryStorageMsg()
        msg.setPrimaryStorageUuid(ps1.getUuid())
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, ps1.uuid)
        MessageReply reply = bus.call(msg)
        assert checked2
        assert !reply.success
        assert Q.New(PrimaryStorageHostRefVO.class)
                .eq(PrimaryStorageHostRefVO_.primaryStorageUuid, ps1.uuid)
                .eq(PrimaryStorageHostRefVO_.status, PrimaryStorageHostStatus.Disconnected)
                .count() == 2L
        assert Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, ps1.uuid)
                .select(PrimaryStorageVO_.status).findValue() == PrimaryStorageStatus.Disconnected
        assert Q.New(HostVO.class).eq(HostVO_.uuid, host.uuid)
                .select(HostVO_.status).findValue() == HostStatus.Connected
    }

    void testAllHostPingNfsSucess(){
        boolean checked = false
        env.simulator(NfsPrimaryStorageKVMBackend.PING_PATH) { HttpEntity<String> e, EnvSpec espec ->
            checked = true
            return new NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse()
        }

        PingPrimaryStorageMsg msg = new PingPrimaryStorageMsg()
        msg.setPrimaryStorageUuid(ps1.getUuid())
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, ps1.uuid)
        bus.send(msg)

        retryInSecs(){
            return {
                assert checked
                assert Q.New(PrimaryStorageHostRefVO.class)
                        .eq(PrimaryStorageHostRefVO_.primaryStorageUuid, ps1.uuid)
                        .eq(PrimaryStorageHostRefVO_.status, PrimaryStorageHostStatus.Connected)
                        .count() == 2L // ps has been reconnected auto, all host-nfs is set to Connected
                assert  Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, ps1.uuid)
                        .select(PrimaryStorageVO_.status).findValue() == PrimaryStorageStatus.Connected
                assert  Q.New(HostVO.class).eq(HostVO_.uuid, host.uuid)
                        .select(HostVO_.status).findValue() == HostStatus.Connected
            }
        }
    }

    void testHostPingAllNfsFail(){
        boolean checked1 = false
        boolean hostCheck1 = false
        boolean checked2 = false
        boolean hostCheck2 = false
        env.simulator(NfsPrimaryStorageKVMBackend.PING_PATH) { HttpEntity<String> e, EnvSpec espec ->
            def cmd = JSONObjectUtil.toObject(e.getBody(), NfsPrimaryStorageKVMBackendCommands.PingCmd.class)
            if (cmd.uuid == ps1.uuid){
                checked1 = true
            } else if (cmd.uuid == ps2.uuid){
                checked2 = true
            }
            def huuid = e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)
            def rsp = new NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse()
            if (huuid == host.getUuid() && cmd.uuid == ps1.uuid){
                hostCheck1 = true
                rsp.setError("on purpose Connection error1")
                rsp.setSuccess(false)
            } else if (huuid == host.getUuid() && cmd.uuid == ps2.uuid){
                hostCheck2 = true
                rsp.setError("on purpose Connection error2")
                rsp.setSuccess(false)
            }
            return rsp
        }

        for (int i = 0; i < retryTimes; i++) {
            PingPrimaryStorageMsg msg = new PingPrimaryStorageMsg()
            msg.setPrimaryStorageUuid(ps1.getUuid())
            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, ps1.uuid)
            MessageReply reply = bus.call(msg)
            assert checked1
            assert reply.success
            if(hostCheck1){
                break
            }
            if(i == retryTimes - 1){
                logger.warn("abandon this testHostPingAllNfsFail because ping ps1 not by selected host after retryTimes")
                return
            }
        }

        for (int i = 0; i < retryTimes; i++) {
            PingPrimaryStorageMsg msg = new PingPrimaryStorageMsg()
            msg.setPrimaryStorageUuid(ps2.getUuid())
            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, ps2.uuid)
            MessageReply reply = bus.call(msg)
            assert checked2
            assert reply.success
            if(hostCheck2){
                break
            }
            if(i == retryTimes - 1){
                logger.warn("abandon this testHostPingAllNfsFail because ping ps2 not by selected host after retryTimes")
                return
            }
        }

        retryInSecs{
            assert Q.New(HostVO.class).eq(HostVO_.uuid, host.uuid).select(HostVO_.status).findValue() == HostStatus.Disconnected
        }

    }
}
