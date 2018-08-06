package org.zstack.test.integration.kvm.vm

import org.zstack.compute.vm.VmQuotaConstant
import org.zstack.core.cloudbus.CloudBusGlobalProperty
import org.zstack.core.db.Q
import org.zstack.header.identity.AccountType
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.image.ImageQuotaConstant
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
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
        //CloudBusGlobalProperty.HTTP_ALWAYS = true

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

                image {
                    name = "vr"
                    url  = "http://zstack.org/download/vr.qcow2"
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
                    attachL2Network("l2-2")
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

                l2NoVlanNetwork {
                    name = "l2-2"
                    physicalInterface = "eth1"

                    l3Network {
                        name = "l3-vr"

                        service {
                            provider = VirtualRouterConstant.PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(), NetworkServiceType.DNS.toString()]
                        }

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.110.100"
                            netmask = "255.255.0.0"
                            gateway = "192.168.100.1"
                        }
                    }

                    l3Network {
                        name = "pubL3"

                        ip {
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
                        }
                    }
                }

                virtualRouterOffering {
                    name = "vr"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
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
            def flatl3 = env.inventoryByName("l3") as L3NetworkInventory
            def vrl3 = env.inventoryByName("l3-vr") as L3NetworkInventory
            testCreateVMWithQuota()
            //testCreateVMConcurrently(1000, flatl3.uuid)
            //testCreateVMConcurrently(300, vrl3.uuid)
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
    void testCreateVMConcurrently(int numberOfVM, String l3Uuid) {
        def instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        def image = env.inventoryByName("image") as ImageInventory

        def list = []
        def existingVM = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.state, VmInstanceState.Running).count()

        for (int i = 0; i < numberOfVM; i++) {
            def thread = Thread.start {
                VmInstanceInventory inv = createVmInstance {
                    name = "test-vm"
                    instanceOfferingUuid = instanceOffering.uuid
                    imageUuid = image.uuid
                    l3NetworkUuids = [l3Uuid]
                } as VmInstanceInventory

                assert Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, inv.uuid).eq(VmInstanceVO_.state, VmInstanceState.Running).isExists()
            }

            list.add(thread)
        }

        list.each { it.join() }

        retryInSecs(25, 3) {
            assert Q.New(VmInstanceVO.class).eq(VmInstanceVO_.type, "UserVm").eq(VmInstanceVO_.state, VmInstanceState.Running).count() == existingVM + numberOfVM
        }

    }
}
