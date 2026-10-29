package org.zstack.test.integration.networkservice.provider.flat.userdata

import org.apache.commons.codec.digest.Sha2Crypt
import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceVO
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.flat.FlatUserdataBackend
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.*
import org.zstack.utils.data.SizeUnit

import java.nio.charset.StandardCharsets

/**
 * Created by miao on 17-7-12.
 */
class LimitUserdataTagAmountCase extends SubCase {
    EnvSpec env

    VmInstanceInventory vm
    L3NetworkInventory l3
    ImageInventory image
    InstanceOfferingInventory offering
    String userdata = "this test user data"
    String userdata2 = "this test user data2"

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

                    kvm {
                        name = "kvm1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                nfsPrimaryStorage {
                    name = "local"
                    url = "127.0.0.1:/nfs"
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

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        env.create {
            l3 = (env.specByName("l3") as L3NetworkSpec).inventory
            image = (env.specByName("image") as ImageSpec).inventory
            offering = (env.specByName("instanceOffering") as InstanceOfferingSpec).inventory

            testSetMoreThanOneUserdataWhenCreateVm()
            testAddMoreUserdataTag()
            testEncryptRootPasswordAndUserdataChpasswd()
        }
    }

    void testEncryptRootPasswordAndUserdataChpasswd() {
        String plainPassword = "PlainRootPass!23"
        String secondPassword = "SecondUserPass!45"
        String preHashed = "\$6\$rounds=4096\$saltsalt\$existinghashedvalue"
        String preHashedYescrypt = "\$y\$j9T\$saltsalt\$existingyescrypthashvalue"
        String rawUserdata = "#cloud-config\r\n" +
                "chpasswd:\r\n" +
                "  list: |\r\n" +
                "      root:${plainPassword}\r\n" +
                "      admin:${preHashed}\r\n" +
                "      ops:${preHashedYescrypt}\r\n" +
                "      guest:RANDOM\r\n" +
                "  expire: False\r\n" +
                "runcmd:\r\n" +
                "  - echo keep:colon\r\n"
        String encodedUserdata = Base64.getEncoder().encodeToString(rawUserdata.getBytes(StandardCharsets.UTF_8))
        FlatUserdataBackend.ApplyUserdataCmd applyCmd = null
        FlatUserdataBackend.BatchApplyUserdataCmd batchApplyCmd = null

        env.afterSimulator(FlatUserdataBackend.APPLY_USER_DATA) { rsp, HttpEntity<String> e ->
            applyCmd = json(e.body, FlatUserdataBackend.ApplyUserdataCmd.class)
            return rsp
        }
        env.afterSimulator(FlatUserdataBackend.BATCH_APPLY_USER_DATA) { rsp, HttpEntity<String> e ->
            batchApplyCmd = json(e.body, FlatUserdataBackend.BatchApplyUserdataCmd.class)
            return rsp
        }

        HostInventory host = env.inventoryByName("kvm")

        VmInstanceInventory vm1 = createVmInstance {
            name = "vm-enc-1"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            hostUuid = host.uuid
            systemTags = [
                VmSystemTags.USERDATA.instantiateTag([(VmSystemTags.USERDATA_TOKEN): encodedUserdata]),
                VmSystemTags.ROOT_PASSWORD.instantiateTag([(VmSystemTags.ROOT_PASSWORD_TOKEN): plainPassword])
            ]
        }

        assert applyCmd != null
        assert applyCmd.userdata.userdataList.size() == 2
        String appliedUserdata = applyCmd.userdata.userdataList[0]
        String appliedRootPwdConfig = applyCmd.userdata.userdataList[1]

        assert !appliedUserdata.contains(plainPassword)
        assert !appliedRootPwdConfig.contains(plainPassword)
        assert appliedUserdata.contains("admin:${preHashed}")
        assert appliedUserdata.contains("ops:${preHashedYescrypt}")
        assert appliedUserdata.contains("guest:RANDOM")
        assert appliedUserdata.contains("  - echo keep:colon")

        def matchers = (appliedRootPwdConfig =~ /(?m)^\s*root:(\$6\$([a-zA-Z0-9.\/]{16})\$[a-zA-Z0-9.\/]{86})\r?$/)
        assert matchers.find()
        String fullHash1 = matchers.group(1)
        String salt1 = matchers.group(2)
        assert Sha2Crypt.sha512Crypt(plainPassword.getBytes(StandardCharsets.UTF_8), "\$6\$" + salt1) == fullHash1
        assert appliedUserdata.contains("      root:${fullHash1}\r\n")

        List<String> initialUserdataList = new ArrayList<>(applyCmd.userdata.userdataList)
        applyCmd = null
        rebootVmInstance {
            uuid = vm1.uuid
        }
        assert applyCmd != null
        assert applyCmd.userdata.userdataList == initialUserdataList

        batchApplyCmd = null
        reconnectHost {
            uuid = host.uuid
        }
        assert batchApplyCmd != null
        def vm1BatchTo = batchApplyCmd.userdata.find { it.metadata.vmUuid == vm1.uuid }
        assert vm1BatchTo != null
        assert vm1BatchTo.userdataList == initialUserdataList

        String seqUserdata = "#cloud-config\n" +
                "chpasswd:\n" +
                "  list: [root:${plainPassword}, testuser:${secondPassword}, ops:${preHashedYescrypt}]\n" +
                "  expire: False\n"
        String encodedSeqUserdata = Base64.getEncoder().encodeToString(seqUserdata.getBytes(StandardCharsets.UTF_8))
        applyCmd = null
        VmInstanceInventory vm2 = createVmInstance {
            name = "vm-enc-2"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            hostUuid = host.uuid
            systemTags = [
                VmSystemTags.USERDATA.instantiateTag([(VmSystemTags.USERDATA_TOKEN): encodedSeqUserdata]),
                VmSystemTags.ROOT_PASSWORD.instantiateTag([(VmSystemTags.ROOT_PASSWORD_TOKEN): plainPassword])
            ]
        }
        assert applyCmd != null
        assert applyCmd.userdata.userdataList.size() == 2
        def matchers2 = (applyCmd.userdata.userdataList[1] =~ /(?m)^\s*root:(\$6\$([a-zA-Z0-9.\/]{16})\$[a-zA-Z0-9.\/]{86})$/)
        assert matchers2.find()
        String fullHash2 = matchers2.group(1)
        String salt2 = matchers2.group(2)
        assert salt2 != salt1
        String flowRendered = applyCmd.userdata.userdataList[0]
        assert !flowRendered.contains(plainPassword)
        assert !flowRendered.contains(secondPassword)
        assert flowRendered.contains("  list: [root:${fullHash2}, testuser:\$6\$")
        assert flowRendered.contains(", ops:${preHashedYescrypt}]\n")
        def parsedFlowYaml = new org.yaml.snakeyaml.Yaml().load(flowRendered) as Map
        List flowList = (parsedFlowYaml.get("chpasswd") as Map).get("list") as List
        assert flowList.size() == 3
        assert flowList[0] == "root:${fullHash2}"
        assert (flowList[1] as String).startsWith("testuser:\$6\$")
        assert flowList[2] == "ops:${preHashedYescrypt}"
    }

    @Override
    void clean() {
        env.delete()
    }

    void testSetMoreThanOneUserdataWhenCreateVm() {
        // test set more than one userdata tag when create vm
        CreateVmInstanceAction action = new CreateVmInstanceAction()
        action.name = "vm"
        action.imageUuid = image.uuid
        action.l3NetworkUuids = [l3.uuid]
        action.instanceOfferingUuid = offering.uuid
        action.systemTags = [
                VmSystemTags.USERDATA.instantiateTag([(VmSystemTags.USERDATA_TOKEN): new String(Base64.getEncoder().encode(userdata.getBytes()))]),
                VmSystemTags.USERDATA.instantiateTag([(VmSystemTags.USERDATA_TOKEN): new String(Base64.getEncoder().encode(userdata2.getBytes()))])
        ]
        action.sessionId = env.session.uuid

        CreateVmInstanceAction.Result res = action.call()
        assert res.error != null
    }

    void testAddMoreUserdataTag() {
        // test add more userdata tag when one already exists
        vm = createVmInstance {
            name = "vm"
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = offering.uuid
            systemTags = [VmSystemTags.USERDATA.instantiateTag([
                    (VmSystemTags.USERDATA_TOKEN): new String(Base64.getEncoder().encode(userdata.getBytes()))
            ])]
        } as VmInstanceInventory

        CreateSystemTagAction action2 = new CreateSystemTagAction(
                resourceType: VmInstanceVO.getSimpleName(),
                resourceUuid: vm.uuid,
                tag: VmSystemTags.USERDATA.instantiateTag([
                        (VmSystemTags.USERDATA_TOKEN): new String(Base64.getEncoder().encode(userdata2.getBytes()))
                ]),
                sessionId: currentEnvSpec.session.uuid
        )

        CreateSystemTagAction.Result res2 = action2.call()

        assert res2.error != null
    }

}


