package org.zstack.test.integration.storage.primary.nfs

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.Q
import org.zstack.header.Constants
import org.zstack.header.host.Host
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
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by Administrator on 2017-05-07.
 */
class PingWhenOnlyOneHostAccessNfsUrlCase extends SubCase{
    EnvSpec env
    HostInventory host
    PrimaryStorageInventory ps
    CloudBus bus

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.nfsOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            host = env.inventoryByName("kvm") as HostInventory
            ps = env.inventoryByName("nfs") as PrimaryStorageInventory
            bus = bean(CloudBus.class)
            testNotAllHostPingNfsSuccess()
            testAllHostPingNfsFail()
            testAllHostPingNfsSucess()
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
            if (huuid == host.getUuid()){
                rsp.setError("Connection error1")
                rsp.setSuccess(false)
                rets.add(false)
                return rsp
            }else {
                rets.add(true)
                return rsp
            }
        }

        PingPrimaryStorageMsg msg = new PingPrimaryStorageMsg()
        msg.setPrimaryStorageUuid(ps.getUuid())
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, ps.uuid)
        MessageReply reply = bus.call(msg)
        assert checked1
        assert reply.success
        assert (rets.get(0) && rets.size() == 1) || (!rets.get(0) && rets.size() == 2)
        if(!rets.get(0)){
            assert Q.New(PrimaryStorageHostRefVO.class)
                    .eq(PrimaryStorageHostRefVO_.hostUuid, host.getUuid())
                    .eq(PrimaryStorageHostRefVO_.primaryStorageUuid, ps.uuid)
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
            rsp.setError("Connection error2")
            rsp.setSuccess(false)
            return rsp
        }

        PingPrimaryStorageMsg msg = new PingPrimaryStorageMsg()
        msg.setPrimaryStorageUuid(ps.getUuid())
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, ps.uuid)
        MessageReply reply = bus.call(msg)
        assert checked2
        assert !reply.success
        assert Q.New(PrimaryStorageHostRefVO.class)
                .eq(PrimaryStorageHostRefVO_.primaryStorageUuid, ps.uuid)
                .eq(PrimaryStorageHostRefVO_.status, PrimaryStorageHostStatus.Disconnected)
                .count() == 2L
        assert Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, ps.uuid)
                .select(PrimaryStorageVO_.status).findValue() == PrimaryStorageStatus.Disconnected
    }

    void testAllHostPingNfsSucess(){
        boolean checked = false
        env.simulator(NfsPrimaryStorageKVMBackend.PING_PATH) { HttpEntity<String> e, EnvSpec espec ->
            checked = true
            return new NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse()
        }

        PingPrimaryStorageMsg msg = new PingPrimaryStorageMsg()
        msg.setPrimaryStorageUuid(ps.getUuid())
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, ps.uuid)
        bus.send(msg)

        retryInSecs(){
            return {
                assert checked
                assert Q.New(PrimaryStorageHostRefVO.class)
                        .eq(PrimaryStorageHostRefVO_.primaryStorageUuid, ps.uuid)
                        .eq(PrimaryStorageHostRefVO_.status, PrimaryStorageHostStatus.Connected)
                        .count() == 2L // ps has been reconnected auto, all host-nfs is set to Connected
                assert  Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, ps.uuid)
                        .select(PrimaryStorageVO_.status).findValue() == PrimaryStorageStatus.Connected
            }
        }


    }
}
