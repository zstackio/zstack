package org.zstack.test.integration.longjob

import com.google.gson.Gson
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.longjob.LongJobVO
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.APIMigrateVmMsg
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.*
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by camile on 18-3-7.
 */
class LiveMigrateVmJobCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf
    Gson gson
    VmInstanceInventory vm1
    KVMHostInventory host1
    KVMHostInventory host2

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
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
                    name = "test-iso1"
                    url = "http://zstack.org/download/test.iso"
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

                    kvm {
                        name = "kvm1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "localhost:/nfs"
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
                useRootDiskOffering("diskOffering")
                useHost("kvm")
            }

        }
    }

    @Override
    void test() {
        env.create {
            testLiveMigrateVmLongJobFailure()
            testLiveMigrateVmLongJobSuccess()
        }
    }

    void testLiveMigrateVmLongJobFailure() {
        dbf = bean(DatabaseFacade.class)
        gson = new Gson()
        vm1 = env.inventoryByName("vm") as VmInstanceInventory
        host1 = env.inventoryByName("kvm") as KVMHostInventory
        host2 = env.inventoryByName("kvm1") as KVMHostInventory
        env.simulator(KVMSecurityGroupBackend.SECURITY_GROUP_CLEANUP_UNUSED_RULE_ON_HOST_PATH) {
            return new KVMAgentCommands.CleanupUnusedRulesOnHostResponse()
        }

        assert vm1.hostUuid == host1.uuid


        APIMigrateVmMsg msg = new APIMigrateVmMsg()
        msg.hostUuid = host2.uuid + 1
        msg.vmInstanceUuid = vm1.uuid

        expect(AssertionError.class) {
            submitLongJob {
                jobName = msg.getClass().getSimpleName()
                jobData = gson.toJson(msg)
            }
        }
    }

    void testLiveMigrateVmLongJobSuccess() {
        APIMigrateVmMsg msg = new APIMigrateVmMsg()
        msg.hostUuid = host2.uuid
        msg.vmInstanceUuid = vm1.uuid

        LongJobInventory jobInv = submitLongJob {
            jobName = msg.getClass().getSimpleName()
            jobData = gson.toJson(msg)
        } as LongJobInventory

        assert jobInv.jobName == msg.getClass().getSimpleName()
        assert jobInv.state == LongJobState.Running

        retryInSecs() {
            VmInstanceVO vo = dbf.findByUuid(vm1.uuid, VmInstanceVO.class)
            assert host2.uuid == vo.hostUuid
        }
        retryInSecs() {
            LongJobVO job = dbFindByUuid(jobInv.getUuid(), LongJobVO.class)
            assert job.state.toString() == LongJobState.Succeeded.toString()
        }

    }
}
