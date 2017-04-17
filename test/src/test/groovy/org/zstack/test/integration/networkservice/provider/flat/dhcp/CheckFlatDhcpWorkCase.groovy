package org.zstack.test.integration.networkservice.provider.flat.dhcp

import junit.framework.Assert
import org.springframework.http.HttpEntity
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.flat.FlatNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.CollectionUtils
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.function.Function
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by heathhose on 17-4-1.
 */
class CheckFlatDhcpWorkCase extends SubCase{

    EnvSpec env
    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = FlatNetworkServiceEnv.oneFlatEipEnv()
        env = env{
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
                    url  = "http://zstack.org/download/test.qcow2"
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
                    attachL2Network("l2-2")
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
                    name = "l2-2"
                    physicalInterface = "eth1"

                    l3Network {
                        name = "l3-2"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        ip {
                            startIp = "192.168.200.10"
                            endIp = "192.168.200.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.200.1"
                        }
                    }
                }

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
                useImage("image")
                useDefaultL3Network("l3-1")
                useL3Networks("l3-1","l3-2")
                useInstanceOffering("instanceOffering")
            }
        }
    }

    @Override
    void test() {
        env.create {
            checkDhcpWork()
        }
    }

    void checkDhcpWork(){
        VmInstanceInventory vm = env.inventoryByName("vm")
        final L3NetworkInventory l31 = env.inventoryByName("l3-1")
        final L3NetworkInventory l32 = env.inventoryByName("l3-2");

        VmNicInventory n = CollectionUtils.find(vm.getVmNics(), new Function<VmNicInventory, VmNicInventory>() {
            @Override
            public VmNicInventory call(VmNicInventory arg) {
                return arg.getL3NetworkUuid().equals(l31.getUuid()) ? arg : null;
            }
        });
        assert n.getDeviceId() ==0

        n = CollectionUtils.find(vm.getVmNics(), new Function<VmNicInventory, VmNicInventory>() {
            @Override
            public VmNicInventory call(VmNicInventory arg) {
                return arg.getL3NetworkUuid().equals(l32.getUuid()) ? arg : null;
            }
        });
        assert n.getDeviceId() == 1

        List<FlatDhcpBackend.DhcpInfo> dhcpInfoList = new ArrayList<FlatDhcpBackend.DhcpInfo>()
        env.afterSimulator(FlatDhcpBackend.APPLY_DHCP_PATH) { rsp, HttpEntity<String> e1 ->
            FlatDhcpBackend.ApplyDhcpCmd cmd = JSONObjectUtil.toObject(e1.body, FlatDhcpBackend.ApplyDhcpCmd.class)
            assert null != cmd
            dhcpInfoList.addAll(cmd.dhcp)
            return rsp
        }
        goOn:
        for (FlatDhcpBackend.ApplyDhcpCmd cmd : dhcpInfoList) {
            FlatDhcpBackend.DhcpInfo info = cmd.dhcp.get(0);
            if (!info.isDefaultL3Network && info.hostname != null) {
                Assert.fail(String.format("wrong hostname set. %s", JSONObjectUtil.toJsonString(info)));
            }

            for (VmNicInventory nic : vm.getVmNics()) {
                if (info.ip.equals(nic.getIp()) && info.gateway.equals(nic.getGateway()) && info.netmask.equals(nic.getNetmask())) {
                    break goOn;
                }
            }

            Assert.fail(String.format("nic %s", JSONObjectUtil.toJsonString(cmd.dhcp)));
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
