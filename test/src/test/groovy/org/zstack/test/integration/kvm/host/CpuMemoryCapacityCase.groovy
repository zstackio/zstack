package org.zstack.test.integration.kvm.host

import org.springframework.http.HttpEntity
import org.zstack.compute.host.HostGlobalConfig
import org.zstack.core.db.Q
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMGlobalConfig
import org.zstack.sdk.GetCpuMemoryCapacityAction
import org.zstack.sdk.HostInventory
import org.zstack.test.integration.kvm.hostallocator.AllocatorTest
import org.zstack.test.integration.kvm.hostallocator.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HostSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.SizeUtils
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by camile on 2017/4/10.
 */
class CpuMemoryCapacityCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(AllocatorTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.noVmThreeHostEnv()
    }

    @Override
    void test() {
        env.create {
            KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.updateValue("1G")
            HostGlobalConfig.AUTO_RECONNECT_ON_ERROR.updateValue(false)
            HostGlobalConfig.PING_HOST_INTERVAL.updateValue(5)
            setHostDisconnecedAndGetCorrectlyCpuMemoryCapacity()
        }
    }

    void setHostDisconnecedAndGetCorrectlyCpuMemoryCapacity() {
        HostInventory kvm1Inv = (env.specByName("kvm1") as HostSpec).inventory
        HostInventory kvm2Inv = (env.specByName("kvm2") as HostSpec).inventory
        HostInventory kvm3Inv = (env.specByName("kvm3") as HostSpec).inventory


        KVMAgentCommands.PingCmd pingCmd = null
        env.afterSimulator(KVMConstant.KVM_PING_PATH) { KVMAgentCommands.PingResponse rsp, HttpEntity<String> e ->
            pingCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.PingCmd.class)
            if (pingCmd.hostUuid == kvm1Inv.uuid || pingCmd.hostUuid == kvm2Inv.uuid) {
                rsp.success = false
            } else if(pingCmd.hostUuid == kvm3Inv.uuid) {
                rsp.success = true
                rsp.hostUuid = pingCmd.hostUuid
            } else {
                rsp.success = true
            }
            return rsp
        }

        retryInSecs{
            return {
                String kvm1Status = Q.New(HostVO.class).select(HostVO_.status).eq(HostVO_.uuid,kvm1Inv.uuid).findValue()
                String kvm2Status = Q.New(HostVO.class).select(HostVO_.status).eq(HostVO_.uuid,kvm2Inv.uuid).findValue()
                String kvm3Status = Q.New(HostVO.class).select(HostVO_.status).eq(HostVO_.uuid,kvm3Inv.uuid).findValue()
                assert kvm1Status == HostStatus.Disconnected.toString()
                assert kvm2Status == HostStatus.Disconnected.toString()
                assert kvm3Status == HostStatus.Connected.toString()
            }
        }
        GetCpuMemoryCapacityAction action = new GetCpuMemoryCapacityAction()
        action.all = true
        action.sessionId = adminSession()
        GetCpuMemoryCapacityAction.Result res = action.call()
        assert res.error == null
        long result = res.value.availableMemory
        assert result == SizeUtils.sizeStringToBytes("9G")
    }


}
