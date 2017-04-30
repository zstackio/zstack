package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.gc.GCStatus
import org.zstack.core.gc.GarbageCollectorVO
import org.zstack.header.message.MessageReply
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.StopVmInstanceMsg
import org.zstack.header.vm.VmInstanceConstant
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.ApplianceVmInventory
import org.zstack.sdk.DestroyVmInstanceAction
import org.zstack.sdk.GarbageCollectorInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.*
import org.zstack.utils.data.SizeUnit

/**
 * Created by xing5 on 2017/3/3.
 */
class VmGCCase extends SubCase {
    EnvSpec env

    DatabaseFacade dbf
    CloudBus bus

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    private VmInstanceInventory createGCCandidateDestroyedVm() {

        retryInSecs(6){
            // ApplianceVm
            ApplianceVmInventory vr = queryVirtualRouterVm {
                conditions = []
            }[0]
            return {
                assert vr.state == VmInstanceState.Running.name()
            }
        }

        def vm = createVmInstance {
            name = "the-vm"
            instanceOfferingUuid = (env.specByName("instanceOffering") as InstanceOfferingSpec).inventory.uuid
            imageUuid = (env.specByName("image1") as ImageSpec).inventory.uuid
            l3NetworkUuids = [(env.specByName("l3") as L3NetworkSpec).inventory.uuid]
        } as VmInstanceInventory

        env.afterSimulator(KVMConstant.KVM_DESTROY_VM_PATH) {
            throw new HttpError(403, "on purpose")
        }

        def a = new DestroyVmInstanceAction()
        a.uuid = vm.uuid
        a.sessionId = adminSession()
        DestroyVmInstanceAction.Result res = a.call()
        // because of the GC, confirm the VM is deleted
        assert res.error == null
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Destroyed
        assert dbf.count(GarbageCollectorVO.class) != 0

        return vm
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
                    useImage("vr-image")
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

    void testDeleteVmWhenHostDisconnect() {

        VmInstanceInventory vm = (env.specByName("vm") as VmSpec).inventory

        env.afterSimulator(KVMConstant.KVM_DESTROY_VM_PATH) {
            throw new HttpError(403, "on purpose")
        }

        def a = new DestroyVmInstanceAction()
        a.uuid = vm.uuid
        a.sessionId = adminSession()
        DestroyVmInstanceAction.Result res = a.call()
        // because of the GC, confirm the VM is deleted
        assert res.error == null
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Destroyed

        KVMAgentCommands.DestroyVmCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_DESTROY_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, KVMAgentCommands.DestroyVmCmd.class)
            return rsp
        }

        // the host reconnecting will trigger the GC
        reconnectHost {
            uuid = vm.hostUuid
        }

        GarbageCollectorInventory inv = null

        retryInSecs {
            inv = queryGCJob {
                conditions=["context~=%${vm.uuid}%"]
            }[0]

            return {
                assert cmd != null
                assert cmd.uuid == vm.uuid
                assert inv.status == GCStatus.Done.toString()
            }
        }

        deleteGCJob {
            uuid = inv.uuid
        }
    }

    void testGCJobCancelAfterHostDelete() {
        VmInstanceInventory vm = createGCCandidateDestroyedVm()

        deleteHost {
            uuid = vm.hostUuid
        }

        GarbageCollectorInventory inv = null

        retryInSecs {
            inv = queryGCJob {
                conditions=["context~=%${vm.uuid}%"]
            }[0]

            return { assert inv.status == GCStatus.Done.toString() }
        }

        deleteGCJob {
            uuid = inv.uuid
        }
    }

    void testGCJobCancelAfterVmRecovered() {
        VmInstanceInventory vm = createGCCandidateDestroyedVm()

        recoverVmInstance {
            uuid = vm.uuid
        }

        // the host reconnecting will trigger the GC
        reconnectHost {
            uuid = vm.hostUuid
        }

        KVMAgentCommands.DestroyVmCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_DESTROY_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, KVMAgentCommands.DestroyVmCmd.class)
            return rsp
        }

        GarbageCollectorInventory inv = null

        retryInSecs {
            inv = queryGCJob {
                conditions=["context~=%${vm.uuid}%"]
            }[0]

            return  {
                // no destroy command sent beacuse the vm is recovered
                assert cmd == null
                assert inv.status == GCStatus.Done.toString()
            }
        }

        deleteGCJob {
            uuid = inv.uuid
        }
    }

    @Override
    void test() {
        dbf = bean(DatabaseFacade.class)
        bus = bean(CloudBus.class)

        env.create {
            testDeleteVmWhenHostDisconnect()
            testGCJobCancelAfterVmRecovered()
            testGCJobCancelAfterHostDelete()

            // recreate the host
            env.recreate("kvm")

            testStopVmWhenHostDisconnect()
            testStopVmGCJobCancelAfterVmDeleted()
            testStopVmGCJobCancelAfterHostDeleted()
        }
    }

    private void stopVmWithGCOpen(String vmUuid) {
        StopVmInstanceMsg msg = new StopVmInstanceMsg()
        msg.gcOnFailure = true
        msg.vmInstanceUuid = vmUuid
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmUuid)
        MessageReply reply = bus.call(msg)
        assert reply.success
    }

    private VmInstanceInventory createGCCandidateStoppedVm() {
        

        def vm = createVmInstance {
            name = "the-vm"
            instanceOfferingUuid = (env.specByName("instanceOffering") as InstanceOfferingSpec).inventory.uuid
            imageUuid = (env.specByName("image1") as ImageSpec).inventory.uuid
            l3NetworkUuids = [(env.specByName("l3") as L3NetworkSpec).inventory.uuid]
        } as VmInstanceInventory

        env.afterSimulator(KVMConstant.KVM_STOP_VM_PATH) {
            throw new HttpError(403, "on purpose")
        }

        stopVmWithGCOpen(vm.uuid)

        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Stopped
        assert dbf.count(GarbageCollectorVO.class) != 0

        return vm
    }

    void testStopVmGCJobCancelAfterHostDeleted() {
        VmInstanceInventory vm = createGCCandidateStoppedVm()

        KVMAgentCommands.StopVmCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_STOP_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, KVMAgentCommands.StopVmCmd.class)
            return rsp
        }

        deleteHost {
            uuid = vm.hostUuid
        }

        GarbageCollectorInventory inv = null

        retryInSecs {
            inv = queryGCJob {
                conditions=["context~=%${vm.uuid}%"]
            }[0]

            return {
                assert cmd == null
                assert inv.status == GCStatus.Done.toString()
            }
        }

        deleteGCJob {
            uuid = inv.uuid
        }
    }

    void testStopVmGCJobCancelAfterVmDeleted() {
        VmInstanceInventory vm = createGCCandidateStoppedVm()

        KVMAgentCommands.StopVmCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_STOP_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, KVMAgentCommands.StopVmCmd.class)
            return rsp
        }

        destroyVmInstance {
            uuid = vm.uuid
        }

        GarbageCollectorInventory inv = null

        retryInSecs {
            // the GC job is cancelled
            inv = queryGCJob {
                conditions=["context~=%${vm.uuid}%"]
            }[0]

            return {
                assert cmd == null
                assert inv.status == GCStatus.Done.toString()
            }
        }

        deleteGCJob {
            uuid = inv.uuid
        }
    }

    void testStopVmWhenHostDisconnect() {
        VmInstanceInventory vm = createGCCandidateStoppedVm()

        KVMAgentCommands.StopVmCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_STOP_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, KVMAgentCommands.StopVmCmd.class)
            return rsp
        }

        // reconnect host to trigger the GC
        reconnectHost {
            uuid = vm.hostUuid
        }

        GarbageCollectorInventory inv = null
        retryInSecs {
            inv = queryGCJob {
                conditions=["context~=%${vm.uuid}%"]
            }[0]

            return {
                assert cmd != null
                assert cmd.uuid == vm.uuid
                assert inv.status == GCStatus.Done.toString()
            }
        }

        deleteGCJob {
            uuid = inv.uuid
        }

        // cleanup our vm
        destroyVmInstance {
            uuid = vm.uuid
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
