package org.zstack.test.integration.kvm.scheduler

import org.zstack.core.scheduler.SchedulerConstant
import org.zstack.header.core.scheduler.SchedulerJobVO
import org.zstack.header.core.scheduler.SchedulerTriggerVO
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.SchedulerJobInventory
import org.zstack.sdk.SchedulerTriggerInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import java.sql.Timestamp

/**
 * Created by AlanJager on 2017/6/7.
 */
class SchedulerCase extends SubCase {
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
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
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
                    name = "image1"
                    url = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "vr"
                    url = "http://zstack.org/download/vr.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
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
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
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
                useImage("image1")
                useL3Networks("l3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testSchedulerJobAPI()
            testSchedulerTriggerAPI()
        }
    }
    
    void testSchedulerJobAPI() {
        VmInstanceInventory vm = env.inventoryByName("vm")

        // test create scheduler job
        SchedulerJobInventory startInv = createStartVmInstanceSchedulerJob {
            vmUuid = vm.uuid
            name = "start"
        } as SchedulerJobInventory
        SchedulerJobVO startVO = dbFindByUuid(startInv.getUuid(), SchedulerJobVO.class)
        assert startVO.name == startInv.name

        SchedulerJobInventory rebootInv = createRebootVmInstanceSchedulerJob {
            vmUuid = vm.uuid
            name = "reboot"
        } as SchedulerJobInventory
        SchedulerJobVO rebootVO = dbFindByUuid(rebootInv.getUuid(), SchedulerJobVO.class)
        assert rebootVO.name == rebootInv.name


        SchedulerJobInventory stopInv = createStopVmInstanceSchedulerJob {
            vmUuid = vm.uuid
            name = "stop"
        } as SchedulerJobInventory
        SchedulerJobVO stopVO = dbFindByUuid(stopInv.getUuid(), SchedulerJobVO.class)
        assert stopVO.name == stopInv.name

        SchedulerJobInventory snapshotSchedulerInv = createVolumeSnapshotSchedulerJob {
            name = "snapshot"
            volumeUuid = vm.getRootVolumeUuid()
            snapShotName = "test"
        } as SchedulerJobInventory
        SchedulerJobVO createVolumeSnapshotSchedulerVO = dbFindByUuid(snapshotSchedulerInv.getUuid(), SchedulerJobVO.class)
        assert createVolumeSnapshotSchedulerVO.name == snapshotSchedulerInv.name

        // test update scheduler api
        updateSchedulerJob {
            uuid = startInv.uuid
            name = "new name"
        }
        startVO = dbFindByUuid(startInv.getUuid(), SchedulerJobVO.class)
        assert startVO.name == "new name"

        // test delete scheduler api
        deleteSchedulerJob {
            uuid = stopInv.uuid
        }
        retryInSecs {
            stopVO = dbFindByUuid(stopInv.getUuid(), SchedulerJobVO.class)
            assert stopVO == null
        }
    }

    void testSchedulerTriggerAPI() {
        VmInstanceInventory vm = env.inventoryByName("vm")

        // test create scheduler trigger
        SchedulerTriggerInventory inv = createSchedulerTrigger {
            name = "trigger"
            description = "this is a trigger"
            schedulerInterval = 12222
            repeatCount = 22222
            startTime = new Timestamp(System.currentTimeMillis())
            schedulerType = SchedulerConstant.SIMPLE_TYPE_STRING.toString()
        }
        SchedulerTriggerVO vo = dbFindByUuid(inv.uuid, SchedulerTriggerVO.class)
        assert vo.name == inv.name

        // test update scheduler trigger
        updateSchedulerTrigger {
            uuid = inv.uuid
            name = "new trigger"
            description = "this is a new trigger desc"
        }
        vo = dbFindByUuid(inv.uuid, SchedulerTriggerVO.class)
        assert vo.name == "new trigger"

        // test delete scheduler trigger
        deleteSchedulerTrigger {
            uuid = inv.uuid
        }
        vo = dbFindByUuid(inv.uuid, SchedulerTriggerVO.class)
        assert vo == null
    }
}
