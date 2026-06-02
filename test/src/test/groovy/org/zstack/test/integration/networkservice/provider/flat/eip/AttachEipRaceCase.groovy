package org.zstack.test.integration.networkservice.provider.flat.eip

import org.springframework.http.HttpEntity
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.eip.EipVO
import org.zstack.network.service.flat.FlatEipBackend
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.sdk.AttachEipAction
import org.zstack.sdk.EipInventory
import org.zstack.sdk.StopVmInstanceAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AttachEipRaceCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image"
                    url = "http://zstack.org/download/test.qcow2"
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
                    attachL2Network("l2-pub")
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
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }

                l2NoVlanNetwork {
                    name = "l2-pub"
                    physicalInterface = "eth1"

                    l3Network {
                        name = "pubL3"

                        ip {
                            startIp = "11.168.100.10"
                            endIp = "11.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.100.1"
                        }
                    }
                }

                attachBackupStorage("sftp")

                eip {
                    name = "eip-race"
                    useVip("pubL3")
                }

                eip {
                    name = "eip-fail"
                    useVip("pubL3")
                }
            }

            vm {
                name = "vm-race"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vm-fail"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testStopVmDuringAttachReleasesEip()
            testAttachFailureRollsBackEarlyBinding()
        }
    }

    void testStopVmDuringAttachReleasesEip() {
        EipInventory eip = env.inventoryByName("eip-race") as EipInventory
        VmInstanceInventory vm = env.inventoryByName("vm-race") as VmInstanceInventory
        String nicUuid = vm.vmNics[0].uuid
        String guestIp = vm.vmNics[0].ip
        String sessionId = adminSession()

        CountDownLatch applyEntered = new CountDownLatch(1)
        CountDownLatch releaseApply = new CountDownLatch(1)
        CountDownLatch deleteCalled = new CountDownLatch(1)
        FlatEipBackend.ApplyEipCmd applyCmd = null
        FlatEipBackend.DeleteEipCmd deleteCmd = null

        env.afterSimulator(FlatEipBackend.APPLY_EIP_PATH) { rsp, HttpEntity<String> entity ->
            FlatEipBackend.ApplyEipCmd cmd = json(entity.getBody(), FlatEipBackend.ApplyEipCmd.class)
            if (cmd.eip.eipUuid == eip.uuid) {
                applyCmd = cmd
                applyEntered.countDown()
                assert releaseApply.await(30, TimeUnit.SECONDS)
            }
            return rsp
        }

        env.afterSimulator(FlatEipBackend.DELETE_EIP_PATH) { rsp, HttpEntity<String> entity ->
            FlatEipBackend.DeleteEipCmd cmd = json(entity.getBody(), FlatEipBackend.DeleteEipCmd.class)
            if (cmd.eip.eipUuid == eip.uuid) {
                deleteCmd = cmd
                deleteCalled.countDown()
            }
            return rsp
        }

        Throwable attachFailure = null
        Thread attachThread = Thread.start {
            try {
                AttachEipAction action = new AttachEipAction()
                action.eipUuid = eip.uuid
                action.vmNicUuid = nicUuid
                action.sessionId = sessionId
                def result = action.call()
                if (result.error != null) {
                    attachFailure = new AssertionError(result.error.toString())
                }
            } catch (Throwable t) {
                attachFailure = t
            }
        }

        assert applyEntered.await(30, TimeUnit.SECONDS)
        try {
            retryInSecs {
                EipVO vo = dbFindByUuid(eip.uuid, EipVO.class)
                assert vo.vmNicUuid == nicUuid
                assert vo.guestIp == guestIp
            }
        } finally {
            releaseApply.countDown()
        }

        attachThread.join(30000)

        assert !attachThread.isAlive()
        assert attachFailure == null
        assert applyCmd != null

        StopVmInstanceAction action = new StopVmInstanceAction()
        action.uuid = vm.uuid
        action.sessionId = sessionId
        def result = action.call()

        assert result.error == null
        assert deleteCalled.await(30, TimeUnit.SECONDS)
        assert deleteCmd != null
        assert deleteCmd.eip.eipUuid == eip.uuid
    }

    void testAttachFailureRollsBackEarlyBinding() {
        EipInventory eip = env.inventoryByName("eip-fail") as EipInventory
        VmInstanceInventory vm = env.inventoryByName("vm-fail") as VmInstanceInventory

        env.afterSimulator(FlatEipBackend.APPLY_EIP_PATH) { rsp, HttpEntity<String> entity ->
            FlatEipBackend.ApplyEipCmd cmd = json(entity.getBody(), FlatEipBackend.ApplyEipCmd.class)
            if (cmd.eip.eipUuid == eip.uuid) {
                throw new HttpError(500, "on purpose")
            }
            return rsp
        }

        AttachEipAction action = new AttachEipAction()
        action.eipUuid = eip.uuid
        action.vmNicUuid = vm.vmNics[0].uuid
        action.sessionId = adminSession()
        def result = action.call()

        assert result.error != null
        retryInSecs {
            EipVO vo = dbFindByUuid(eip.uuid, EipVO.class)
            assert vo.vmNicUuid == null
            assert vo.guestIp == null
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
