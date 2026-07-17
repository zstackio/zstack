package org.zstack.test.integration.kvm.host

import org.zstack.core.CoreGlobalProperty
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.kvm.KVMHostUtils
import org.zstack.sdk.HostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.TimeUnit

class LibvirtRestartEchoTimeoutCase extends SubCase {
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
            testCalculateLibvirtRestartEchoTimeout()
            testCountVmsForLibvirtRestartEchoTimeoutExcludesStoppedVm()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testCalculateLibvirtRestartEchoTimeout() {
        int oldTimeout = CoreGlobalProperty.REST_FACADE_ECHO_TIMEOUT
        try {
            CoreGlobalProperty.REST_FACADE_ECHO_TIMEOUT = 60

            assert KVMHostUtils.calculateLibvirtRestartEchoTimeoutMillis(100) == TimeUnit.SECONDS.toMillis(60)
            assert KVMHostUtils.calculateLibvirtRestartEchoTimeoutMillis(101) == TimeUnit.SECONDS.toMillis(61)
            assert KVMHostUtils.calculateLibvirtRestartEchoTimeoutMillis(10000) == TimeUnit.SECONDS.toMillis(180)

            CoreGlobalProperty.REST_FACADE_ECHO_TIMEOUT = 300
            assert KVMHostUtils.calculateLibvirtRestartEchoTimeoutMillis(10000) == TimeUnit.SECONDS.toMillis(300)
        } finally {
            CoreGlobalProperty.REST_FACADE_ECHO_TIMEOUT = oldTimeout
        }
    }

    void testCountVmsForLibvirtRestartEchoTimeoutExcludesStoppedVm() {
        HostInventory host = env.inventoryByName("kvm") as HostInventory
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory

        long originalCount = KVMHostUtils.countVmsForLibvirtRestartEchoTimeout(host.uuid)
        assert originalCount > 0

        VmInstanceState originalState = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.state)
                .eq(VmInstanceVO_.uuid, vm.uuid)
                .findValue()
        try {
            SQL.New(VmInstanceVO.class)
                    .eq(VmInstanceVO_.uuid, vm.uuid)
                    .set(VmInstanceVO_.state, VmInstanceState.Stopped)
                    .update()

            assert KVMHostUtils.countVmsForLibvirtRestartEchoTimeout(host.uuid) == originalCount - 1
        } finally {
            SQL.New(VmInstanceVO.class)
                    .eq(VmInstanceVO_.uuid, vm.uuid)
                    .set(VmInstanceVO_.state, originalState)
                    .update()
        }
    }
}
