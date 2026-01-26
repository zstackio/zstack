package org.zstack.test.integration

import org.zstack.core.db.Q
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.plugin.example.GreetingVO
import org.zstack.plugin.example.GreetingVO_
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

class HelloWorldTest extends SubCase {

    EnvSpec env

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
        spring {
            include("helloworld.xml")
        }

    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(10)
                cpu = 10

                toPublic = true
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

                    toPublic = true
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
                        totalMem = SizeUnit.GIGABYTE.toByte(100)
                        totalCpu = 100
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
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), UserdataConstant.USERDATA_TYPE_STRING,
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "10.168.100.10"
                            endIp = "10.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "10.168.100.1"
                        }

                        toPublic = true
                    }

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
            }
        }

    }

    @Override
    void test() {
        env.create{
            testSayHelloWorldApi()
        }
    }

    void testSayHelloWorldApi(){
        helloWorld {
            greeting = 'qiuyu'
        }

        logger.info(String.format("------------------createGreeting之前----------------------"));
        createGreeting {
            greeting = 'qiuyu-12/18'
        }
        assert Q.New(GreetingVO).eq(GreetingVO_.greeting,"qiuyu-12/18").isExists()
        logger.info(String.format("------------------createGreeting之后---------------------"));

        logger.info(String.format("------------------deleteGreeting之前----------------------"));
        def vo = Q.New(GreetingVO).eq(GreetingVO_.greeting, "qiuyu-12/18").find()
        assert vo!=null
        deleteGreeting {
            uuid = vo.uuid
        }
        logger.info(String.format("------------------deleteGreeting之后，看见这就ok了----------------------"));

        logger.info(String.format("------------------开始验证deleteGreeting是否成功----------------------"));
        assert Q.New(GreetingVO).eq(GreetingVO_.greeting,"qiuyu-12/18").isExists()
        logger.info(String.format("------------------deleteGreeting还是存在，代码错误----------------------"));
    }

    @Override
    void clean(){
        env.delete()
    }

}