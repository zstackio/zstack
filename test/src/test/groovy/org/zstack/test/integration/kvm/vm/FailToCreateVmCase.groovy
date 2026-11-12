package org.zstack.test.integration.kvm.vm

import org.zstack.compute.vm.VmInstanceExtensionPointEmitter
import org.zstack.core.db.Q
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.vm.VmInstanceBeforeStartExtensionPoint
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L2NetworkInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by AlanJager on 2017/5/7.
 */
class FailToCreateVmCase extends SubCase {
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
                        name = "pubL3"

                        ip {
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
                        }
                    }
                }

                l2NoVlanNetwork {
                    name = "l2_2"
                    physicalInterface = "eth1"

                    l3Network {
                        name = "l3"

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
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
        }
    }

    @Override
    void test() {
        env.create {
            createVmWithAL3NetworkWhichNotAttachedToTheCluster()
            createVmWithSystemTagErrors()
        }
    }

    void createVmWithSystemTagErrors() {
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")
        L3NetworkInventory l3 = env.inventoryByName("pubL3")
        VmInstanceExtensionPointEmitter emitter = bean(VmInstanceExtensionPointEmitter)
        def originalExtensions = emitter.VmInstanceBeforeStartExtensions
        def disabled = new ErrorCode(code: "TEST.DEVICE_DISABLED", description: "An operation failed",
                details: "PCI device[uuid:device-a] is not enabled (state:Disabled)")
        def occupied = new ErrorCode(code: "TEST.DEVICE_OCCUPIED", description: "An operation failed",
                details: "PCI device[uuid:device-b] is occupied (vmUuid:other-vm)")
        def descriptionOnly = new ErrorCode(code: "TEST.TAG_REJECTED", description: "System tag is not supported")

        try {
            // Simulate extension failures through the real Create API and its rollback chain.
            [[disabled], [disabled, occupied], [descriptionOnly]].each { List<ErrorCode> errors ->
                emitter.VmInstanceBeforeStartExtensions = errors.collect { ErrorCode error ->
                    return { String vmUuid, List<String> tags -> error } as VmInstanceBeforeStartExtensionPoint
                }
                String vmUuid = org.zstack.core.Platform.uuid
                def action = new CreateVmInstanceAction(
                        name: "system-tag-error", resourceUuid: vmUuid,
                        instanceOfferingUuid: instanceOffering.uuid, imageUuid: image.uuid,
                        l3NetworkUuids: [l3.uuid], systemTags: ["hostname::tag-error-vm"],
                        sessionId: adminSession())
                def result = action.call()

                assert result.error != null
                assert result.error.globalErrorCode == "ORG_ZSTACK_COMPUTE_VM_10241"
                assert result.error.cause != null
                errors.each { ErrorCode error ->
                    assert result.error.details.contains(error.details ?: error.description)
                }
                assert !Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid).isExists()
            }
        } finally {
            emitter.VmInstanceBeforeStartExtensions = originalExtensions
        }
    }

    void createVmWithAL3NetworkWhichNotAttachedToTheCluster() {
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")
        L3NetworkInventory l3 = env.inventoryByName("l3")

        CreateVmInstanceAction action = new CreateVmInstanceAction()
        action.name = "vm"
        action.instanceOfferingUuid = instanceOffering.uuid
        action.imageUuid = image.uuid
        action.l3NetworkUuids = [l3.uuid]
        action.sessionId = adminSession()
        CreateVmInstanceAction.Result result = action.call()
        assert result.error != null
    }
}
