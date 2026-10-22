package org.zstack.test.integration.kvm.nic

import org.zstack.compute.vm.VmFactoryManagerImpl
import org.zstack.core.db.Q
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.OperationFailureException
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.header.vm.VmInstanceNicFactory
import org.zstack.header.vm.VmNicVO
import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

class VmNicFactoryFailureCase extends SubCase {
    EnvSpec env

    @Override
    void setup() { useSpring(KvmTest.springSpec) }

    @Override
    void environment() { env = Env.oneVmBasicEnv() }

    @Override
    void clean() { env.delete() }

    @Override
    void test() {
        env.create {
            def manager = bean(VmFactoryManagerImpl)
            def field = VmFactoryManagerImpl.getDeclaredField('vmInstanceNicFactories')
            field.accessible = true
            Map factories = field.get(manager) as Map
            VmInstanceNicFactory original = factories['VNIC']
            assert original != null
            def error = new ErrorCode('VM.1000', 'injected NIC factory failure')
            error.globalErrorCode = 'ORG_ZSTACK_COMPUTE_VM_10346'
            error.details = 'injected factory rejection after first NIC allocation'
            def failure = new OperationFailureException(error)
            int calls = 0
            def replacement = [
                    getType: { original.type },
                    createVmNic: { nic, spec ->
                        if (++calls == 2) { throw failure }
                        return original.createVmNic(nic, spec)
                    }
            ] as VmInstanceNicFactory
            long beforeNics = Q.New(VmNicVO).count()
            long beforeIps = Q.New(UsedIpVO).count()
            factories['VNIC'] = replacement
            try {
                def result = new CreateVmInstanceAction(sessionId: env.session.uuid,
                        name: 'factory-failure', imageUuid: env.inventoryByName('image1').uuid,
                        instanceOfferingUuid: env.inventoryByName('instanceOffering').uuid,
                        l3NetworkUuids: [env.inventoryByName('pubL3').uuid, env.inventoryByName('l3').uuid],
                        defaultL3NetworkUuid: env.inventoryByName('l3').uuid).call()
                assert calls == 2 : JSONObjectUtil.toJsonString(result.error)
                assert result.error != null
                String returned = JSONObjectUtil.toJsonString(result.error)
                assert returned.contains(error.globalErrorCode)
                assert returned.contains(error.details)
                assert Q.New(VmNicVO).count() == beforeNics
                assert Q.New(UsedIpVO).count() == beforeIps
            } finally {
                factories['VNIC'] = original
            }
        }
    }
}
