package org.zstack.test.integration.kvm.vm

import org.zstack.compute.vm.VmQuotaConstant
import org.zstack.compute.vm.VmQuotaUtil
import org.zstack.core.db.SQL
import org.zstack.header.identity.AccountConstant
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.sdk.GetAccountQuotaUsageResult
import org.zstack.sdk.QuotaUsage
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class VmQuotaUsageExcludePodCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

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
            testContainerPodIsNotCountedAsVm()
        }
    }

    Map<String, Long> vmUsage() {
        GetAccountQuotaUsageResult result = getAccountQuotaUsage {
            uuid = AccountConstant.INITIAL_SYSTEM_ADMIN_UUID
        } as GetAccountQuotaUsageResult
        List<QuotaUsage> usages = result.usages as List<QuotaUsage>
        return usages.findAll { it.name.startsWith("vm.") }.collectEntries { [(it.name): it.used] }
    }

    void setVmType(String vmUuid, String type) {
        SQL.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid).set(VmInstanceVO_.type, type).update()
    }

    void testContainerPodIsNotCountedAsVm() {
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        Map<String, Long> withUserVm = vmUsage()
        VmQuotaUtil.VmQuota allocatorWithUserVm = new VmQuotaUtil().getUsedVmCpuMemory(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)
        assert withUserVm[VmQuotaConstant.VM_RUNNING_NUM] >= 1L

        setVmType(vm.uuid, "Pod")
        Map<String, Long> withPod = vmUsage()
        VmQuotaUtil.VmQuota allocatorWithPod = new VmQuotaUtil().getUsedVmCpuMemory(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)

        // a container Pod is accounted by container.* quotas, counting it in vm.* makes a project without VMs look over quota
        assert withPod[VmQuotaConstant.VM_TOTAL_NUM] == withUserVm[VmQuotaConstant.VM_TOTAL_NUM] - 1
        assert withPod[VmQuotaConstant.VM_RUNNING_NUM] == withUserVm[VmQuotaConstant.VM_RUNNING_NUM] - 1
        assert withPod[VmQuotaConstant.VM_RUNNING_CPU_NUM] == withUserVm[VmQuotaConstant.VM_RUNNING_CPU_NUM] - vm.cpuNum
        assert withPod[VmQuotaConstant.VM_RUNNING_MEMORY_SIZE] == withUserVm[VmQuotaConstant.VM_RUNNING_MEMORY_SIZE] - vm.memorySize
        assert allocatorWithPod.totalVmNum == allocatorWithUserVm.totalVmNum - 1
        assert allocatorWithPod.runningVmNum == allocatorWithUserVm.runningVmNum - 1
        assert allocatorWithPod.runningVmCpuNum == allocatorWithUserVm.runningVmCpuNum - vm.cpuNum
        assert allocatorWithPod.runningVmMemorySize == allocatorWithUserVm.runningVmMemorySize - vm.memorySize

        setVmType(vm.uuid, vm.type)
        assert vmUsage() == withUserVm
    }
}
