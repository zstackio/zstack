package org.zstack.test.integration.kvm.hostallocator

import org.zstack.kvm.KVMGlobalConfig
import org.zstack.testlib.*
/**
 * Created by david on 3/6/17.
 */
class LeastVmPreferredAllocatorCase extends SubCase {
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
        env = Env.fourVmThreeHostEnv()
    }

    @Override
    void test() {
        env.create {
            testCreateMulitpleVMs(8)
        }
    }

    void testCreateMulitpleVMs(Long num) {
        KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.updateValue("2G")

        // We have three hosts, each has 8 CPU and 10G memory.
        // The reserved memory for each host is 1GB.
        //   host1 has 4 VMs  (instance offering: 2 CPU, 2GB)
        //   host2 has no VM
        //   host3 has no VM
        // We can still create 8 VMs in all, on host2 and host3, with the instance offering.
        def thisImageUuid = (env.specByName("image1") as ImageSpec).inventory.uuid
        def instOffering = (env.specByName("instanceOffering") as InstanceOfferingSpec).inventory.uuid
        def l3uuid = (env.specByName("pubL3") as L3NetworkSpec).inventory.uuid

        def threads = []
        1.upto(num, {
            def vmName = "VM-${it}".toString()
            def thread = Thread.start {
                createVmInstance {
                    name = vmName
                    instanceOfferingUuid = instOffering
                    imageUuid = thisImageUuid
                    l3NetworkUuids = [l3uuid]
                }
            }

            threads.add(thread)
        })

        threads.each{it.join()}

        // We shall not be able to create another VM instance now
        try {
            createVmInstance {
                name = "VMM"
                instanceOfferingUuid = instOffering
                imageUuid = thisImageUuid
                l3NetworkUuids = [l3uuid]
            }
        } catch (AssertionError ignored) {
            // Upon API failure, we have an assertion error in ApiHelper.groovy
            return
        }

        assert false
    }
}
