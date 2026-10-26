package org.zstack.test.integration.kvm.host

import org.zstack.core.db.Q
import org.zstack.core.thread.ThreadFacade
import org.zstack.kvm.KVMAgentCommands.GetVirtualizerInfoRsp
import org.zstack.kvm.KVMAgentCommands.VirtualizerInfoTO
import org.zstack.kvm.hypervisor.KvmHypervisorInfoManager
import org.zstack.kvm.hypervisor.datatype.KvmHypervisorInfoVO
import org.zstack.kvm.hypervisor.datatype.KvmHypervisorInfoVO_
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

class KvmHypervisorInfoConcurrentSaveCase extends SubCase {
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
            VmInstanceInventory vm = env.inventoryByName("vm")
            KvmHypervisorInfoManager manager = bean(KvmHypervisorInfoManager.class)
            ThreadFacade threads = bean(ThreadFacade.class)
            KvmHypervisorInfoVO host = Q.New(KvmHypervisorInfoVO.class)
                    .eq(KvmHypervisorInfoVO_.uuid, vm.hostUuid).find()
            assert host != null
            VirtualizerInfoTO hostInfo = new VirtualizerInfoTO(
                    uuid: host.uuid, virtualizer: host.hypervisor, version: host.version)
            VirtualizerInfoTO vmInfo = new VirtualizerInfoTO(
                    uuid: vm.uuid, virtualizer: host.hypervisor, version: host.version)
            GetVirtualizerInfoRsp rsp = new GetVirtualizerInfoRsp(
                    hostInfo: hostInfo, vmInfoList: [vmInfo])
            retryInSecs {
                assert Q.New(KvmHypervisorInfoVO.class).eq(KvmHypervisorInfoVO_.uuid, vm.uuid).isExists()
            }
            manager.clean(vm.uuid)
            assert !Q.New(KvmHypervisorInfoVO.class).eq(KvmHypervisorInfoVO_.uuid, vm.uuid).isExists()

            CountDownLatch start = new CountDownLatch(1)
            List<Closure> reports = [
                    { manager.save(rsp) },
                    { manager.saveHostInfo(hostInfo) }
            ]
            List<Future> saves = (0..<12).collect { int index ->
                FutureTask<Void> task = new FutureTask<Void>({
                    assert start.await(30, TimeUnit.SECONDS)
                    reports[index % reports.size()].call()
                    return null
                } as Callable<Void>)
                threads.submitTimeoutTask(task, TimeUnit.MILLISECONDS, 0)
                return task
            }

            start.countDown()
            saves.each { it.get(30, TimeUnit.SECONDS) }

            assert Q.New(KvmHypervisorInfoVO.class).eq(KvmHypervisorInfoVO_.uuid, vm.uuid).count() == 1
            assert Q.New(KvmHypervisorInfoVO.class).eq(KvmHypervisorInfoVO_.uuid, host.uuid).count() == 1
            KvmHypervisorInfoVO saved = Q.New(KvmHypervisorInfoVO.class)
                    .eq(KvmHypervisorInfoVO_.uuid, vm.uuid).find()
            assert saved.version == vmInfo.version
            assert saved.hypervisor == vmInfo.virtualizer

            vmInfo.version = "4.2.0-628.g36ee592.el7"
            manager.save(rsp)
            assert Q.New(KvmHypervisorInfoVO.class).eq(KvmHypervisorInfoVO_.uuid, vm.uuid)
                    .select(KvmHypervisorInfoVO_.version).findValue() == vmInfo.version

            stopVmInstance { uuid = vm.uuid }
            assert !Q.New(KvmHypervisorInfoVO.class).eq(KvmHypervisorInfoVO_.uuid, vm.uuid).isExists()
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
