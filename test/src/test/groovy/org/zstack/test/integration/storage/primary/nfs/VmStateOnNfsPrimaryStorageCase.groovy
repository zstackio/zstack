package org.zstack.test.integration.storage.primary.nfs

import org.springframework.http.HttpEntity
import org.zstack.compute.host.HostGlobalConfig
import org.zstack.core.cloudbus.CloudBus
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.host.ConnectHostMsg
import org.zstack.header.host.ConnectHostReply
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.image.ImageConstant
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ReconnectHostAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by AlanJager on 2017/6/2.
 */
class VmStateOnNfsPrimaryStorageCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
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
                    url  = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "vr"
                    url  = "http://zstack.org/download/vr.qcow2"
                }

                image {
                    name = "iso"
                    url  = "http://zstack.org/download/test.iso"
                    format = ImageConstant.ISO_FORMAT_STRING.toString()
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
                        managementIp = "127.0.0.2"
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
                useDiskOfferings("diskOffering")
                useHost("kvm")
            }
        }
    }

    @Override
    void test() {
        env.create {
            HostGlobalConfig.AUTO_RECONNECT_ON_ERROR.updateValue(false)
            HostGlobalConfig.PING_HOST_INTERVAL.updateValue(1)
            testDeleteHostVmWillStop()
        }
    }

    void testDeleteHostVmWillStop() {
        HostInventory host = env.inventoryByName("kvm") as HostInventory
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory

        env.afterSimulator(KVMConstant.KVM_PING_PATH) { KVMAgentCommands.PingResponse rsp, HttpEntity<String> e ->
            KVMAgentCommands.PingCmd pingCmd = JSONObjectUtil.toObject(e.body,  KVMAgentCommands.PingCmd.class)
            if (pingCmd.hostUuid == vm.hostUuid) {
                rsp.success = false
                rsp.setError("on purpose")
            } else {
                rsp.success = true
            }
            rsp.hostUuid = pingCmd.hostUuid
            return rsp
        }

        env.message(ConnectHostMsg.class) { ConnectHostMsg msg, CloudBus bus ->
            ConnectHostReply reply = new ConnectHostReply()
            reply.success = false
            reply.setError(new ErrorCode())
            bus.reply(msg, reply)
        }

        // make host disconnected
        ReconnectHostAction action = new ReconnectHostAction()
        action.uuid = host.uuid
        action.sessionId = adminSession()
        ReconnectHostAction.Result ret = action.call()
        assert ret.error != null
        HostVO hostVO
        retryInSecs {
            hostVO = dbFindByUuid(host.uuid, HostVO.class) as HostVO
            assert hostVO.status == HostStatus.Disconnected
        }

        VmInstanceVO vo
        retryInSecs {
            vo = dbFindByUuid(vm.uuid, VmInstanceVO.class) as VmInstanceVO
            assert vo.state == VmInstanceState.Unknown
        }

        deleteHost {
            uuid = host.uuid
        }

        retryInSecs {
            vo = dbFindByUuid(vm.uuid, VmInstanceVO.class) as VmInstanceVO
            assert vo.state == VmInstanceState.Stopped
        }

    }
}
