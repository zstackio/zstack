package org.zstack.test.integration.networkservice.provider.flat.dhcp

import org.springframework.http.HttpEntity
import org.zstack.core.thread.ThreadFacade
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VirtualRouterVmInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class VerifyPrepareDhcpWhenReconnectHostCase extends SubCase {
    EnvSpec env
    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env{
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(1)
                cpu = 2
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
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2-1")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2-1"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3-1"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

                        service {
                            provider = VyosConstants.PROVIDER_TYPE
                            types = [EipConstant.EIP_NETWORK_SERVICE_TYPE,
                                     NetworkServiceType.DNS.toString(),
                                     NetworkServiceType.SNAT.toString()]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }

                    l3Network {
                        name = "pubL3-1"
                        category = "Public"

                        ip {
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
                        }
                    }

                }

                virtualRouterOffering {
                    name = "vr-offering"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3-1")
                    usePublicL3Network("pubL3-1")
                    useImage("vr")
                }

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
                useImage("image")
                useDefaultL3Network("l3-1")
                useL3Networks("l3-1")
                useInstanceOffering("instanceOffering")
            }
        }
    }

    @Override
    void test() {
        env.create {
            checkDhcpWork()
            testBatchStartVmApplyDhcp()
        }
    }

    void checkDhcpWork(){
        def host = queryHost {}[0] as HostInventory
        def vm = env.inventoryByName("vm") as VmInstanceInventory
        def vmItemTokens = new LinkedHashSet<String>()

        setVmHostname {
            uuid = vm.uuid
            hostname = "test-name"
        }

        def called = 0
        FlatDhcpBackend.BatchApplyDhcpCmd cmd = null
        env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) { rsp, HttpEntity<String> e1 ->
            cmd = JSONObjectUtil.toObject(e1.body, FlatDhcpBackend.BatchApplyDhcpCmd.class)
            called += 1
            cmd.dhcpInfos.each { info ->
                info.dhcp.each { dhcp ->
                    vmItemTokens.add(String.format("%s-%s-%s", dhcp.ip, dhcp.netmask, dhcp.gateway))
                }
            }
            return rsp
        }

        stopVmInstance {
            uuid = vm.uuid
        }
        startVmInstance {
            uuid = vm.uuid
        }
        assert called == 1
        assert cmd.dhcpInfos.size() == 1
        assert cmd.dhcpInfos.get(0).dhcp.get(0).hostname == "test-name"
        def vmNic = vm.vmNics.get(0)
        def expectedToken = String.format("%s-%s-%s", vmNic.ip, vmNic.netmask, vmNic.gateway)
        assert vmItemTokens.contains(expectedToken)

        called = 0
        cmd = null
        vmItemTokens.clear()
        reconnectHost { uuid=host.uuid }
        assert called == 1
        assert cmd.dhcpInfos.get(0).dhcp.get(0).hostname == "test-name"
        assert vmItemTokens.contains(expectedToken)

        def vr = queryVirtualRouterVm {}[0] as VirtualRouterVmInventory
        assert vr != null

        destroyVmInstance { uuid=vm.uuid }

        reconnectHost { uuid=host.uuid }
        assert called == 1
    }

    void testBatchStartVmApplyDhcp() {
        L3NetworkInventory l3 = env.inventoryByName("l3-1") as L3NetworkInventory
        ImageInventory image = env.inventoryByName("image") as ImageInventory
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory

        def vmCount = 4
        def vms = new ArrayList<VmInstanceInventory>()
        def hostnameByIp = new LinkedHashMap<String, String>()
        (0..<vmCount).each { idx ->
            def hname = "batch-${idx}"
            VmInstanceInventory inv = createVmInstance {
                name = "batch-vm-${idx}"
                imageUuid = image.uuid
                l3NetworkUuids = [l3.uuid]
                instanceOfferingUuid = offering.uuid
            } as VmInstanceInventory
            setVmHostname {
                uuid = inv.uuid
                hostname = hname
            }
            hostnameByIp.put(inv.vmNics.get(0).ip, hname)
            vms.add(inv)
        }

        vms.each { vmInv ->
            stopVmInstance {
                uuid = vmInv.uuid
            }
        }

        def batchCmds = Collections.synchronizedList(new ArrayList<FlatDhcpBackend.BatchApplyDhcpCmd>())
        def firstBatchArrived = new CountDownLatch(1)
        def releaseFirstBatch = new CountDownLatch(1)
        env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) { rsp, HttpEntity<String> e1 ->
            FlatDhcpBackend.BatchApplyDhcpCmd cmd = JSONObjectUtil.toObject(e1.body, FlatDhcpBackend.BatchApplyDhcpCmd.class)
            batchCmds.add(cmd)
            if (batchCmds.size() == 1) {
                firstBatchArrived.countDown()
                releaseFirstBatch.await(10, TimeUnit.SECONDS)
            }
            return rsp
        }

        VmInstanceInventory blocker = vms.remove(0)
        new Thread({
            startVmInstance {
                uuid = blocker.uuid
            }
        }).start()
        assert firstBatchArrived.await(10, TimeUnit.SECONDS)

        CountDownLatch doneLatch = new CountDownLatch(vms.size())
        vms.each { vmInv ->
            new Thread({
                try {
                    startVmInstance {
                        uuid = vmInv.uuid
                    }
                } finally {
                    doneLatch.countDown()
                }
            }).start()
        }

        ThreadFacade thdf = bean(ThreadFacade.class)
        retryInSecs {
            assert thdf.getChainTaskInfo(String.format("coalesce-queue-flat-dhcp-apply-%s", vms[0].hostUuid)).pendingTask.size() == 3
        }

        releaseFirstBatch.countDown()
        assert doneLatch.await(2, TimeUnit.MINUTES)

        retryInSecs(5) {
            assert batchCmds.size() == 2
        }
        retryInSecs(2) {
            assert batchCmds.size() == 2
        }

        Closure<Set<String>> toTokenSet = { FlatDhcpBackend.BatchApplyDhcpCmd batch ->
            def tokens = new LinkedHashSet<String>()
            batch.dhcpInfos.each { info ->
                info.dhcp.each { dhcp ->
                    tokens.add(String.format("%s-%s-%s", dhcp.ip, dhcp.netmask, dhcp.gateway))
                }
            }
            return tokens
        }

        Closure<Map<String, String>> toHostnameMap = { FlatDhcpBackend.BatchApplyDhcpCmd batch ->
            def hostnames = new LinkedHashMap<String, String>()
            batch.dhcpInfos.each { info ->
                info.dhcp.each { dhcp ->
                    hostnames.put(dhcp.ip, dhcp.hostname)
                }
            }
            return hostnames
        }

        def firstBatchTokens = toTokenSet(batchCmds.get(0))
        def secondBatchTokens = toTokenSet(batchCmds.get(1))
        def firstBatchHostnames = toHostnameMap(batchCmds.get(0))
        def secondBatchHostnames = toHostnameMap(batchCmds.get(1))

        def blockerNic = blocker.vmNics.get(0)
        def blockerToken = String.format("%s-%s-%s", blockerNic.ip, blockerNic.netmask, blockerNic.gateway)
        assert firstBatchTokens.size() == 1
        assert firstBatchTokens.contains(blockerToken)
        assert firstBatchHostnames.size() == 1
        assert firstBatchHostnames.get(blockerNic.ip) == hostnameByIp.get(blockerNic.ip)

        def expectedTokens = new LinkedHashSet<String>()
        def expectedHostnames = new LinkedHashMap<String, String>()
        vms.each { vmInv ->
            def nic = vmInv.vmNics.get(0)
            expectedTokens.add(String.format("%s-%s-%s", nic.ip, nic.netmask, nic.gateway))
            expectedHostnames.put(nic.ip, hostnameByIp.get(nic.ip))
        }
        assert secondBatchTokens.containsAll(expectedTokens)
        assert secondBatchTokens.size() == expectedTokens.size()
        assert secondBatchHostnames == expectedHostnames
    }

    @Override
    void clean() {
        env.delete()
    }
}
