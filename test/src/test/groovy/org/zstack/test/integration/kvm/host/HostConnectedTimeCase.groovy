package org.zstack.test.integration.kvm.host

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.host.*
import org.zstack.header.tag.SystemTagVO
import org.zstack.header.tag.SystemTagVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.HostInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.header.network.l2.BatchCheckNetworkPhysicalInterfaceMsg
import org.zstack.header.network.l2.BatchCheckNetworkPhysicalInterfaceReply
import org.zstack.core.cloudbus.CloudBus

class HostConnectedTimeCase extends SubCase {

    EnvSpec env

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
            testHostConnectedTime()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testHostConnectedTime() {

        HostInventory host = env.inventoryByName("kvm")

        assert Q.New(SystemTagVO.class)
                .eq(SystemTagVO_.resourceUuid, host.uuid)
                .eq(SystemTagVO_.resourceType, HostVO.getSimpleName())
                .like(SystemTagVO_.tag, "ConnectedTime::%")
                .isExists()

        env.afterSimulator(KVMConstant.KVM_CONNECT_PATH) { rsp, HttpEntity<String> e ->
            throw new Exception("on purpose")
        }

        int sizeFlag = 0
        env.message(BatchCheckNetworkPhysicalInterfaceMsg.class) { BatchCheckNetworkPhysicalInterfaceMsg msg, CloudBus bus ->

            sizeFlag = msg.physicalInterfaces.size()

            BatchCheckNetworkPhysicalInterfaceReply reply = new BatchCheckNetworkPhysicalInterfaceReply()
            bus.reply(msg, reply)
        }

        expect(AssertionError.class) {
            reconnectHost {
                uuid = host.uuid
            }
        }

        assert Q.New(SystemTagVO.class)
                .eq(SystemTagVO_.resourceUuid, host.uuid)
                .eq(SystemTagVO_.resourceType, HostVO.getSimpleName())
                .like(SystemTagVO_.tag, "ConnectedTime::%")
                .count() == 0

        KVMAgentCommands.ConnectCmd connectCmd = null
        env.afterSimulator(KVMConstant.KVM_CONNECT_PATH) { KVMAgentCommands.AgentResponse rsp, HttpEntity<String> e ->
            connectCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.ConnectCmd.class)
            rsp.success = false
            if (connectCmd.hostUuid == host.uuid) {
                rsp.success = true
            }
            return rsp
        }

        reconnectHost {
            uuid = host.uuid
        }

        String tag_ = Q.New(SystemTagVO.class)
                .select(SystemTagVO_.tag)
                .eq(SystemTagVO_.resourceUuid, host.uuid)
                .eq(SystemTagVO_.resourceType, HostVO.class.getSimpleName())
                .like(SystemTagVO_.tag, "ConnectedTime::%")
                .findValue()

        assert tag_ != null

        reconnectHost {
            uuid = host.uuid
        }

        String tag = Q.New(SystemTagVO.class)
                .select(SystemTagVO_.tag)
                .eq(SystemTagVO_.resourceUuid, host.uuid)
                .eq(SystemTagVO_.resourceType, HostVO.class.getSimpleName())
                .like(SystemTagVO_.tag, "ConnectedTime::%")
                .findValue()

        assert tag == tag_

        deleteHost{
            uuid = host.uuid
        }

        assert Q.New(SystemTagVO.class)
                .eq(SystemTagVO_.resourceUuid, host.uuid)
                .eq(SystemTagVO_.resourceType, HostVO.getSimpleName())
                .like(SystemTagVO_.tag, "ConnectedTime::%")
                .count() == 0

        assert sizeFlag == 1
    }
}