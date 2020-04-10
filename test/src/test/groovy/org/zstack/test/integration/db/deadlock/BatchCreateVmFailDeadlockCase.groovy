package org.zstack.test.integration.db.deadlock

import com.google.common.collect.Lists
import org.hibernate.exception.LockAcquisitionException
import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.core.Platform
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.header.vo.ResourceVO
import org.zstack.header.vo.ResourceVO_
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.sdk.*
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test
import org.zstack.utils.data.SizeUnit
import org.zstack.core.db.Q
import java.util.concurrent.atomic.AtomicInteger
import org.zstack.core.db.SQL

/**
 * Created by lining on 2020/04/09.
 */
class BatchCreateVmFailDeadlockCase extends SubCase{
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
                memory = SizeUnit.GIGABYTE.toByte(1)
                cpu = 1
            }

            diskOffering {
                name = "diskOffering"
                    diskSize = SizeUnit.GIGABYTE.toByte(1)
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "iso"
                    url  = "http://zstack.org/download/test.iso"
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
                        totalCpu = 1000
                        totalMem = SizeUnit.GIGABYTE.toByte(10000)
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                    availableCapacity = SizeUnit.GIGABYTE.toByte(100000)
                    totalCapacity = SizeUnit.GIGABYTE.toByte(10000)
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        ip {
                            startIp = "192.168.0.2"
                            endIp = "192.168.255.0"
                            netmask = "255.255.0.0"
                            gateway = "192.168.0.1"
                        }
                    }
                }

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testBatchCreateVm()
        }
    }

    void testBatchCreateVm() {
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        PrimaryStorageInventory ps = env.inventoryByName("local")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        ImageInventory image = env.inventoryByName("iso")
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")

        String num = System.getProperty("num")
        int numberOfResources = num == null ? 3000 : Integer.parseInt(num)
        List<String> mockResourceUuids = []
        List<ResourceVO> mockResourceVOs = []
        for (int i = 0 ; i < numberOfResources; i++) {
            String resourceUuid = Platform.getUuid()
            mockResourceUuids.add(resourceUuid)

            List<String> args = [resourceUuid, "mock by case", "Mock"]
            ResourceVO resourceVO = new ResourceVO(args.toArray())
            resourceVO.setConcreteResourceType("Mock")
            mockResourceVOs.add(resourceVO)
        }

        List resourceVOParts = Lists.partition(mockResourceVOs, 5000)
        resourceVOParts.stream().forEach({ List resourceVOS ->
               dbf.persistCollection(resourceVOS)
        })

        env.afterSimulator(FlatDhcpBackend.APPLY_DHCP_PATH) {FlatDhcpBackend.ApplyDhcpRsp rsp, HttpEntity<java.lang.String> e ->
            Random r = new Random()
            int ran = r.nextInt(2)
            if (ran == 0) {
                rsp.setError("case mock error")
            }

            return rsp
        }

        AtomicInteger count = new AtomicInteger(0)
        boolean dbDeadlockOccurred = false
        def threads = []
        for (int i = 0; i < 100; i++) {
            def thread = Thread.start {
                CreateVmInstanceAction action = new CreateVmInstanceAction(
                        name : "test-" + i,
                        instanceOfferingUuid : instanceOffering.uuid,
                        l3NetworkUuids : [l3.uuid],
                        imageUuid : image.uuid,
                        rootDiskOfferingUuid : diskOffering.uuid,
                        dataDiskOfferingUuids: [diskOffering.uuid],
                        sessionId: Test.currentEnvSpec.session.uuid,
                        //systemTags: [VmSystemTags.CREATE_WITHOUT_CD_ROM.instantiateTag([(VmSystemTags.CREATE_WITHOUT_CD_ROM_TOKEN): true])]
                )

                CreateVmInstanceAction.Result ret = action.call()
                count.incrementAndGet()
                if (ret.error != null) {
                    if (ret.error.getDetails().contains(LockAcquisitionException.class.simpleName)) {
                        dbDeadlockOccurred = true
                    }
                }
            }

            threads.add(thread)
        }

        threads.each { it.join() }

        retryInSecs(40) {
            assert count.get() >= 100
        }
        assert !dbDeadlockOccurred

        List<String> vmUuids = Q.New(VmInstanceVO.class)
            .select(VmInstanceVO_.uuid)
            .listValues()
        for (String vmUuid : vmUuids) {
            destroyVmInstance {
                uuid = vmUuid
            }
        }

        List resourceUuidParts = Lists.partition(mockResourceUuids, 1000)
        resourceUuidParts.stream().forEach({ List resourceUuids ->
            SQL.New(ResourceVO.class)
                .in(ResourceVO_.uuid, resourceUuids)
                .delete()
        })
    }
}
