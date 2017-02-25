import org.zstack.core.cloudbus.CloudBus
import org.zstack.header.host.ConnectHostMsg
import org.zstack.kvm.KVMConstant
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.test.deployer.Deployer
import org.zstack.testlib.DiskOfferingSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.premium.TestPremium
import org.zstack.utils.data.SizeUnit

/**
 * Created by xing5 on 2017/2/12.
 * 1. 2
 * 3
 */
class Test3 extends TestPremium {
    boolean success
    DiskOfferingSpec diskOfferingSpec
    EnvSpec envSpec

    @Override
    void setup() {
        spring {
            nfsPrimaryStorage()
            kvm()
            vyos()
            eip()
            sftpBackupStorage()
            portForwarding()
            lb()
            ipsec()
            ceph()
            smp()
            localStorage()
            securityGroup()
        }

        message(ConnectHostMsg.class, { false }) { msg, CloudBus bus ->
            throw new Exception("on purpose")
        }
    }

    @Override
    void environment() {
        envSpec = env {
            account {
                name = "xin"
                password = "password"
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(10)
                useAccount("xin")
            }

            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
                useAccount("xin")
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
                    useAccount("xin")
                }

                image {
                    name = "vr"
                    url  = "http://zstack.org/download/vr.qcow2"
                    useAccount("xin")
                }
            }

            cephBackupStorage {
                name = "ceph-bk"
                fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@localhost:23", "root:password@127.0.0.1:23"]

                image {
                    name = "image2"
                    url  = "http://zstack.org/download/test.qcow2"
                    useAccount("xin")
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
                        usedMem = 1000
                        totalCpu = 10
                    }

                    attachPrimaryStorage("nfs", "ceph-pri", "local", "smp")
                    attachL2Network("l2")
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "localhost:/nfs"
                }

                cephPrimaryStorage {
                    name = "ceph-pri"
                    fsid="7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls = ["root:password@localhost/?monPort=7777", "root:password@127.0.0.1/?monPort=7777"]
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                smpPrimaryStorage {
                    name = "smp"
                    url = "/smp"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"
                        useAccount("xin")

                        service {
                            provider = "vrouter"
                            types = ["DHCP", "DNS", "Eip", "SNAT", "PortForwarding", "LoadBalancer", "IPsec"]
                        }

                        service {
                            provider = "SecurityGroup"
                            types = ["SecurityGroup"]
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
                        useAccount("xin")

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
                    useAccount("xin")
                }

                attachBackupStorage("sftp")

                eip {
                    name = "eip"
                    useVip("pubL3")
                    useVmNic("vm", "l3")
                    useAccount("xin")
                }

                portForwarding {
                    name = "pf"
                    vipPortStart = 22
                    privatePortStart = 22
                    protocolType = "TCP"
                    useVip("pubL3")
                    useVmNic("vm", "l3")
                    useAccount("xin")
                }

                lb {
                    name = "lb"
                    useVip("pubL3")
                    useAccount("xin")

                    listener {
                        protocol = "tcp"
                        loadBalancerPort = 22
                        instancePort = 22
                        useVmNic("vm", "l3")
                    }
                }

                ipsec {
                    name = "ipsec"
                    peerAddress = "1.1.1.1"
                    peerCidrs = ["10.10.0.0/24"]
                    useVip("pubL3")
                    useL3Network("l3")
                }

                securityGroup {
                    name = "sg"
                    attachL3Network("l3")
                    useAccount("xin")

                    rule {
                        type = "Ingress"
                        startPort = 100
                        endPort = 110
                        protocol = "TCP"
                    }

                    useVmNic("vm", "l3")
                }
            }

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
                useAccount("xin")
            }
        }

        Deployer.simulator(KVMConstant.KVM_CONNECT_PATH) {
            throw new HttpError(403, "on purpose")
        }

        Deployer.afterSimulator(LocalStorageKvmBackend.INIT_PATH) { LocalStorageKvmBackend.AgentResponse rsp ->
            rsp.totalCapacity = SizeUnit.GIGABYTE.toByte(1)
            rsp.availableCapacity = SizeUnit.GIGABYTE.toByte(1)
            return rsp
        }

        envSpec.create()
    }

    @Override
    void test() {
        assert local.inventory.availableCapacity == SizeUnit.GIGABYTE.toByte(1)
        assert local.inventory.totalCapacity == SizeUnit.GIGABYTE.toByte(1)
    }
}
