package org.zstack.test.integration.kvm.vm

import org.zstack.compute.vm.VmQuotaConstant
import org.zstack.core.db.Q
import org.zstack.header.identity.AccountType
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.image.ImageQuotaConstant
import org.zstack.sdk.*
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import java.util.concurrent.atomic.AtomicInteger
///**
// * Created by kayo on 2018/3/20.
// */
class CreateVmConcurrentlyCase extends SubCase {
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
        env = makeEnv {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(1)
                cpu = 1
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(20)
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image"
                    url  = "http://zstack.org/download/test.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "host1"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                        totalMem = SizeUnit.GIGABYTE.toByte(1000)
                        totalCpu = 1000
                    }

                    kvm {
                        name = "host2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                        totalMem = SizeUnit.GIGABYTE.toByte(1000)
                        totalCpu = 1000
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.110.100"
                            netmask = "255.255.0.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("host1")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testCreateVMWithQuota()
//            testCreateVMConcurrently(1000)
        }
    }

    void testCreateVMWithQuota() {
        def existingVM = Q.New(VmInstanceVO.class).count()
        def runningVmQuota = 4
        def imageNumQuota = 1

        def userpass = "password"
        def newAccount = createAccount {
            name = "normaluser1"
            password = userpass
            type = AccountType.Normal.toString()
        } as AccountInventory

        String vmName = "test-vm-quota"
        def instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        def image = env.inventoryByName("image") as ImageInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory

        shareResource {
            resourceUuids = [ instanceOffering.uuid, image.uuid, l3.uuid]
            accountUuids = [newAccount.uuid]
        }

        updateQuota {
            identityUuid = newAccount.uuid
            name = VmQuotaConstant.VM_TOTAL_NUM
            value = runningVmQuota + 1
        }

        updateQuota {
            identityUuid = newAccount.uuid
            name = VmQuotaConstant.VM_RUNNING_NUM
            value = runningVmQuota
        }

        updateQuota {
            identityUuid = newAccount.uuid
            name = ImageQuotaConstant.IMAGE_NUM
            value = imageNumQuota
        }

        def list = []

        SessionInventory userSessionInv = logInByAccount {
            accountName = newAccount.name
            password = userpass
        } as SessionInventory

        for (int i = 0; i < runningVmQuota+1; i++) {
            def thread = Thread.start {
                try {
                    createVmInstance {
                        name = vmName
                        instanceOfferingUuid = instanceOffering.uuid
                        imageUuid = image.uuid
                        l3NetworkUuids = [l3.uuid]
                        sessionId = userSessionInv.uuid
                    } as VmInstanceInventory
                } catch (AssertionError ignored) {
                }
            }

            list.add(thread)
        }

        list.each { it.join() }

        assert Q.New(VmInstanceVO.class).count() == existingVM + runningVmQuota
        def vmUuid = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.name, vmName).limit(1).select(VmInstanceVO_.uuid).listValues().get(0) as String
        stopVmInstance {
            uuid = vmUuid
            sessionId = userSessionInv.uuid
        }

        createVmInstance {
            name = vmName
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            sessionId = userSessionInv.uuid
        }

        def hasError = false
        try {
            startVmInstance {
                uuid = vmUuid
            }
        } catch (AssertionError ignored) {
            assert ignored.toString().contains(VmQuotaConstant.VM_RUNNING_NUM)
            hasError = true
        }

        assert hasError

        // stop all running VMs of normaluser1
        List<VmInstanceInventory> vms = queryVmInstance {
            sessionId = userSessionInv.uuid
        } as List<VmInstanceInventory>

        vms.each {
            def curUuid = it.uuid
            if (it.state == VmInstanceState.Running.toString()) {
                stopVmInstance {
                    uuid = curUuid
                }
            }
        }

        list.clear()
        AtomicInteger successCount = new AtomicInteger()
        vms.each {
            def thisUuid = it.uuid
            def thread = Thread.start {
                try {
                    startVmInstance {
                        uuid = thisUuid
                        sessionId = userSessionInv.uuid
                    }

                    successCount.incrementAndGet()
                } catch (AssertionError ignored) {
                }
            }

            list.add(thread)
        }

        list.each { it.join() }

        assert successCount.get() == runningVmQuota

        def cnt = 0
        hasError = false
        for (int i = 0; i < imageNumQuota+1; ++i) {
            try {
                createRootVolumeTemplateFromRootVolume {
                    name = "vm-template"
                    rootVolumeUuid = vms.get(0).getRootVolumeUuid()
                    sessionId = userSessionInv.uuid
                }
                cnt += 1
            } catch (AssertionError ignored) {
                hasError = true
            }
        }

        assert hasError
        assert cnt == imageNumQuota

        hasError = false
        try {
            createDataVolumeTemplateFromVolume {
                name = "data-template"
                volumeUuid = vms.get(0).getRootVolumeUuid()
                sessionId = userSessionInv.uuid
            }
        } catch (AssertionError ignored) {
            hasError = true
        }

        assert hasError
    }

    // This case is for ZSTAC-8576
    // PR system will met API timeout (api timeout is 25s)
    // Can be execute separately if needed
    void testCreateVMConcurrently(int numberOfVM) {
        def instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        def image = env.inventoryByName("image") as ImageInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory

        def list = []
        def existingVM = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.state, VmInstanceState.Running).count()

        for (int i = 0; i < numberOfVM; i++) {
            def thread = Thread.start {
                VmInstanceInventory inv = createVmInstance {
                    name = "test-vm"
                    instanceOfferingUuid = instanceOffering.uuid
                    imageUuid = image.uuid
                    l3NetworkUuids = [l3.uuid]
                } as VmInstanceInventory

                assert Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, inv.uuid).eq(VmInstanceVO_.state, VmInstanceState.Running).isExists()
            }

            list.add(thread)
        }

        list.each { it.join() }

        retryInSecs(25, 3) {
            assert Q.New(VmInstanceVO.class).eq(VmInstanceVO_.state, VmInstanceState.Running).count() == existingVM + numberOfVM
        }

    }
}
