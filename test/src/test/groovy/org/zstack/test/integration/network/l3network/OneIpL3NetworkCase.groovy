package org.zstack.test.integration.network.l3network

import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.ImageSpec
import org.zstack.testlib.InstanceOfferingSpec
import org.zstack.testlib.L3NetworkSpec
import org.zstack.testlib.SubCase
import static java.util.Arrays.asList


/**
 * Created by camile on 2017/4/11.
 */
class OneIpL3NetworkCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
        useSpring(KvmTest.springSpec)

    }
    @Override
    void environment() {
        env = Env.OneIpL3Network()
    }

    @Override
    void test() {
        env.create {
            createVmSuccessOnOneIpL3Network()
            add3IpRangeToL3Andcreate3VmSuccessButCreateOneMoreVmFailure()
        }
    }

    void createVmSuccessOnOneIpL3Network() {
        InstanceOfferingSpec  ioSpec= env.specByName("instanceOffering")
        ImageSpec iSpec = env.specByName("image1")
        L3NetworkSpec l3Spec = env.specByName("l3")
        createVmInstance {
            name = "vm"
            instanceOfferingUuid = ioSpec.inventory.uuid
            imageUuid = iSpec.inventory.uuid
            l3NetworkUuids = asList((l3Spec.inventory.uuid))
        }
    }
    void add3IpRangeToL3Andcreate3VmSuccessButCreateOneMoreVmFailure() {
        InstanceOfferingSpec  ioSpec= env.specByName("instanceOffering")
        ImageSpec iSpec = env.specByName("image1")
        L3NetworkSpec l3Spec = env.specByName("l3")
        addIpRange{
            l3NetworkUuid = l3Spec.inventory.uuid
            name = "test2"
            startIp = "192.168.100.11"
            endIp = "192.168.100.11"
            netmask = "255.255.255.0"
            gateway = "192.168.100.1"
        }
        addIpRange{
            l3NetworkUuid = l3Spec.inventory.uuid
            name = "test3"
            startIp = "192.168.100.12"
            endIp = "192.168.100.12"
            netmask = "255.255.255.0"
            gateway = "192.168.100.1"
        }
        addIpRange{
            l3NetworkUuid = l3Spec.inventory.uuid
            name = "test4"
            startIp = "192.168.100.13"
            endIp = "192.168.100.13"
            netmask = "255.255.255.0"
            gateway = "192.168.100.1"
        }
        createVmInstance {
            name = "vm2"
            instanceOfferingUuid = ioSpec.inventory.uuid
            imageUuid = iSpec.inventory.uuid
            l3NetworkUuids = asList((l3Spec.inventory.uuid))
        }
        createVmInstance {
            name = "vm3"
            instanceOfferingUuid = ioSpec.inventory.uuid
            imageUuid = iSpec.inventory.uuid
            l3NetworkUuids = asList((l3Spec.inventory.uuid))
        }
        createVmInstance {
            name = "vm4"
            instanceOfferingUuid = ioSpec.inventory.uuid
            imageUuid = iSpec.inventory.uuid
            l3NetworkUuids = asList((l3Spec.inventory.uuid))
        }
        CreateVmInstanceAction createVmInstanceAction = new  CreateVmInstanceAction()
        createVmInstanceAction.name ="vm5"
        createVmInstanceAction.instanceOfferingUuid = ioSpec.inventory.uuid
        createVmInstanceAction.imageUuid = iSpec.inventory.uuid
        createVmInstanceAction.l3NetworkUuids = asList((l3Spec.inventory.uuid))
        createVmInstanceAction.sessionId = adminSession()
        CreateVmInstanceAction.Result res = createVmInstanceAction.call()
        res.error != null
    }
}


