package org.zstack.test.integration.kvm.vm

import org.zstack.core.asyncbatch.While
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.core.NoErrorCompletion
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceConstant
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Created by heathhose on 17-4-18.
 */
class ConcurrentOperateVmCase extends SubCase{
    EnvSpec env
    DatabaseFacade dbf
    long numberOfVm = 1000
    def vmNameList
    Map<String,String> map

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.MEGABYTE.toByte(512)
                cpu = 1
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
                    name = "vr-image"
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
                        totalCpu = 120
                        totalMem = SizeUnit.TERABYTE.toByte(1)
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
                            startIp = "192.168.0.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.0.0"
                            gateway = "192.168.0.1"
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
                    useImage("vr-image")
                }

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        dbf = bean(DatabaseFacade.class)
        map = new ConcurrentHashMap<String,String>()
        vmNameList = []
        env.create {
            testCreateVmConcurrently()
            testDeleteVmConcurrently()
            testExpungeVmConcurrently()
        }
    }

    void testCreateVmConcurrently(){
        def l3nw = env.inventoryByName("l3") as L3NetworkInventory
        def image = env.inventoryByName("image1") as ImageInventory
        def offer = env.inventoryByName("instanceOffering") as InstanceOfferingInventory

        for (idx in 1..numberOfVm) {
            vmNameList.add("VM-${idx}".toString())

        }

        new While<>(vmNameList).all(new While.Do() {
            @Override
            void accept(Object item, NoErrorCompletion completion) {
                try {
                    def vm = createVmInstance {
                        name = item
                        instanceOfferingUuid = offer.uuid
                        imageUuid = image.uuid
                        l3NetworkUuids = [l3nw.uuid]
                    } as VmInstanceInventory
                    map.putIfAbsent(item as String,vm.uuid)
                } catch (AssertionError ignored) {}
            }
        }).run(new NoErrorCompletion(){
            @Override
            void done() {
                assert map.size() == 1000
                def numberOfCreateVm = dbf.count(VmInstanceVO.class)
                assert numberOfCreateVm == numberOfVm + 1l
            }
        })

    }

    void testDeleteVmConcurrently(){
        new While<>(vmNameList).all(new While.Do() {
            @Override
            void accept(Object item, NoErrorCompletion completion) {
                try {
                    destroyVmInstance {
                        uuid = map.get(item)
                    }
                } catch (AssertionError ignored) {}
            }
        }).run(new NoErrorCompletion(){
            @Override
            void done() {
                def numberOfDeleteVm = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.state,VmInstanceState.Destroyed).count()
                assert numberOfDeleteVm == numberOfVm
            }
        })

    }

    void testExpungeVmConcurrently(){
        new While<>(vmNameList).all(new While.Do() {
            @Override
            void accept(Object item, NoErrorCompletion completion) {
                try {
                    expungeVmInstance {
                        uuid = map.get(item)
                    }
                } catch (AssertionError ignored) {}
            }
        }).run(new NoErrorCompletion(){
            @Override
            void done() {
                def numberOfRemainVm = dbf.count(VmInstanceVO.class)
                assert numberOfRemainVm == 1l
            }
        })

    }

    @Override
    void clean() {
        env.delete()
    }
}
