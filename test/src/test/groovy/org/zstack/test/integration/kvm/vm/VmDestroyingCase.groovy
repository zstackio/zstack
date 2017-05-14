package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.compute.host.HostGlobalConfig
import org.zstack.compute.host.HostTrackImpl
import org.zstack.compute.host.HostTracker
import org.zstack.compute.vm.VmTracer
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.core.NoErrorCompletion
import org.zstack.header.host.CheckVmStateOnHypervisorMsg
import org.zstack.header.host.CheckVmStateOnHypervisorReply
import org.zstack.header.host.HostVO
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstance
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMGlobalConfig
import org.zstack.kvm.KVMHostInventory
import org.zstack.kvm.KvmVmSyncPingTask
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.DestroyVmInstanceAction
import org.zstack.sdk.HostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by AlanJager on 2017/4/24.
 */
class VmDestroyingCase extends SubCase {
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
            testDestroyingVmWillBeSetToDestroyedIfDestroyingFailButVmSyncMsgReturnStopped()
        }
    }

    void testDestroyingVmWillBeSetToDestroyedIfDestroyingFailButVmSyncMsgReturnStopped() {
        VmInstanceInventory vm = env.inventoryByName("vm")
        String sessionId = adminSession()

        KVMAgentCommands.DestroyVmCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_DESTROY_VM_PATH) { KVMAgentCommands.DestroyVmResponse rsp, HttpEntity<String> e->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.DestroyVmCmd.class)
            rsp.success = false
            rsp.error = "no purpose"
            return rsp
        }

        env.message(CheckVmStateOnHypervisorMsg.class) { CheckVmStateOnHypervisorMsg msg, CloudBus bus ->
            def reply = new CheckVmStateOnHypervisorReply()
            def list = new HashMap<String, String>()
            list.put(vm.uuid, VmInstanceState.Stopped.toString())
            reply.setStates(list)
            reply.success = true
            bus.reply(msg, reply)
        }

        new Thread(new Runnable() {
            @Override
            void run() {
                DestroyVmInstanceAction action = new DestroyVmInstanceAction()
                action.uuid = vm.uuid
                action.sessionId = sessionId
                DestroyVmInstanceAction.Result result = action.call()

                assert result.error != null
            }
        }).start()

        retryInSecs {
            VmInstanceVO vo = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid).list().get(0)

            assert vo.state == VmInstanceState.Destroyed
            assert cmd != null
        }

        recoverVmInstance {
            uuid = vm.uuid
        }

        startVmInstance {
            uuid = vm.uuid
        }

        VmInstanceVO vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Running

        env.cleanSimulatorAndMessageHandlers()
    }
}
