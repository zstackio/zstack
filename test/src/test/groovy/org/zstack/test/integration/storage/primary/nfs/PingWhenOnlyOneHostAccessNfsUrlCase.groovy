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
            testNotAllHostPingNfsSuccess()
        }

    }

    @Override
    void clean() {
        env.delete()
    }

    void testNotAllHostPingNfsSuccess(){
        HostInventory host = env.inventoryByName("kvm") as HostInventory
        HostInventory host1 = env.inventoryByName("kvm1") as HostInventory
        PrimaryStorageInventory ps = env.inventoryByName("nfs") as PrimaryStorageInventory

        CloudBus bus = bean(CloudBus.class)
        List<Boolean> rets = Collections.synchronizedList(new ArrayList<Boolean>())

        env.simulator(NfsPrimaryStorageKVMBackend.PING_PATH) { HttpEntity<String> e, EnvSpec espec ->
            def cmd = JSONObjectUtil.toObject(e.getBody(), NfsPrimaryStorageKVMBackendCommands.PingCmd.class)
            def huuid = e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)
            def rsp = new NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse()
            if (huuid == host.getUuid()){
                rsp.setError("Connection error")
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
        assert reply.success
        assert (rets.get(0) && rets.size() == 1) || (!rets.get(0) && rets.size() == 2)
        if(!rets.get(0)){
            assert retryInSecs(){
                return Q.New(HostVO.class).eq(HostVO_.uuid, host.getUuid()).select(HostVO_.status).findValue() == HostStatus.Disconnected
            }
        }

    }

}
