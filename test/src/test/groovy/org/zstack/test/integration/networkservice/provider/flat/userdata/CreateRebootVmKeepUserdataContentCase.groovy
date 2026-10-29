package org.zstack.test.integration.networkservice.provider.flat.userdata

import org.apache.commons.codec.digest.Sha2Crypt
import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.network.service.flat.FlatUserdataBackend
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.flat.FlatNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.nio.charset.StandardCharsets

class CreateRebootVmKeepUserdataContentCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = FlatNetworkServiceEnv.oneHostNoVmEnv()
    }

    @Override
    void test() {
        env.create {
            testCreateAndRebootKeepUserdataContent()
            testCreateAndRebootEncryptRootPasswordAndUserdataChpasswd()
        }
    }

    void testCreateAndRebootKeepUserdataContent() {
        String userdata = "#cloud-config\nruncmd:\n - echo keep-userdata-content\n"
        String encodedUserdata = new String(Base64.getEncoder().encode(userdata.getBytes()))
        FlatUserdataBackend.ApplyUserdataCmd applyCmd = null

        env.afterSimulator(FlatUserdataBackend.APPLY_USER_DATA) { rsp, HttpEntity<String> e ->
            applyCmd = json(e.body, FlatUserdataBackend.ApplyUserdataCmd.class)
            return rsp
        }

        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image")
        L3NetworkInventory l3 = env.inventoryByName("l3")

        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            systemTags = [VmSystemTags.USERDATA.instantiateTag([(VmSystemTags.USERDATA_TOKEN): encodedUserdata])]
        }

        assertUserdataNotModified(applyCmd, userdata)

        applyCmd = null
        rebootVmInstance {
            uuid = vm.uuid
        }

        assertUserdataNotModified(applyCmd, userdata)

        destroyVmInstance {
            uuid = vm.uuid
        }
    }

    void testCreateAndRebootEncryptRootPasswordAndUserdataChpasswd() {
        String plainPassword = "PlainRootPassword123!"
        String plainPassword2 = "SecondUserPassword456!"
        String literalBackslashQuotePwd = "Quote\\\"Pass123!"
        String literalDoubleSingleQuotePwd = "Quote''Pass123!"
        String preHashed = "\$6\$fixedsalt1234567\$alreadyhashedvalue"
        String preHashedYescrypt = "\$y\$j9T\$abcdefghijklmnop\$AZu7ErwCzb9FJfltyqhBjFrGOXXJVNtfqZ1NUjAswcC"
        String userdata = "#cloud-config\r\n" +
                "chpasswd:\r\n" +
                "  list: |\r\n" +
                "      root:${plainPassword}\r\n" +
                "      literal_dq:${literalBackslashQuotePwd}\r\n" +
                "      literal_sq:${literalDoubleSingleQuotePwd}\r\n" +
                "      admin:${preHashed}\r\n" +
                "      yescrypt_user:${preHashedYescrypt}\r\n" +
                "      guest:RANDOM\r\n" +
                "  expire: False\r\n" +
                "runcmd:\r\n" +
                "  - echo keep:colon\r\n"
        String encodedUserdata = Base64.getEncoder().encodeToString(userdata.getBytes(StandardCharsets.UTF_8))
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

        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        HostInventory host = env.inventoryByName("kvm")

        VmInstanceInventory vm1 = createVmInstance {
            name = "vm-enc-1"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
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
        assert !appliedUserdata.contains(literalBackslashQuotePwd)
        assert !appliedUserdata.contains(literalDoubleSingleQuotePwd)
        assert !appliedRootPwdConfig.contains(plainPassword)
        assert appliedUserdata.contains("admin:${preHashed}")
        assert appliedUserdata.contains("yescrypt_user:${preHashedYescrypt}")
        assert appliedUserdata.contains("guest:RANDOM")
        assert appliedUserdata.contains("  - echo keep:colon")

        def matchers = (appliedRootPwdConfig =~ /(?m)^\s*root:(\$6\$([a-zA-Z0-9.\/]{16})\$[a-zA-Z0-9.\/]{86})\r?$/)
        assert matchers.find()
        String fullHash1 = matchers.group(1)
        String salt1 = matchers.group(2)
        assert Sha2Crypt.sha512Crypt(plainPassword.getBytes(StandardCharsets.UTF_8), "\$6\$" + salt1) == fullHash1
        assert appliedUserdata.contains("      root:${fullHash1}\r\n")

        def literalDqMatcher = (appliedUserdata =~ /(?m)^\s*literal_dq:(\$6\$([a-zA-Z0-9.\/]{16})\$[a-zA-Z0-9.\/]{86})\r?$/)
        assert literalDqMatcher.find()
        assert Sha2Crypt.sha512Crypt(literalBackslashQuotePwd.getBytes(StandardCharsets.UTF_8), "\$6\$" + literalDqMatcher.group(2)) == literalDqMatcher.group(1)

        def literalSqMatcher = (appliedUserdata =~ /(?m)^\s*literal_sq:(\$6\$([a-zA-Z0-9.\/]{16})\$[a-zA-Z0-9.\/]{86})\r?$/)
        assert literalSqMatcher.find()
        assert Sha2Crypt.sha512Crypt(literalDoubleSingleQuotePwd.getBytes(StandardCharsets.UTF_8), "\$6\$" + literalSqMatcher.group(2)) == literalSqMatcher.group(1)

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

        String inlineSeqUserdata = "#cloud-config\n" +
                "chpasswd:\n" +
                "  list: [\"root:Quote\\\"Pass123!\", 'zstactest:Quote''Pass123!', testuser:${plainPassword2}]\n" +
                "  expire: False\n"
        String encodedSeqUserdata = Base64.getEncoder().encodeToString(inlineSeqUserdata.getBytes(StandardCharsets.UTF_8))
        applyCmd = null
        VmInstanceInventory vm2 = createVmInstance {
            name = "vm-enc-2"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
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
        String appliedInlineUserdata = applyCmd.userdata.userdataList[0]
        assert !appliedInlineUserdata.contains("Quote")
        assert !appliedInlineUserdata.contains(plainPassword2)
        Map<?, ?> parsedYaml = new org.yaml.snakeyaml.Yaml().load(appliedInlineUserdata) as Map<?, ?>
        List<String> parsedList = (parsedYaml.get("chpasswd") as Map<?, ?>).get("list") as List<String>
        assert parsedList.size() == 3
        def dqMatcher = (parsedList[0] =~ /^root:(\$6\$([a-zA-Z0-9.\/]{16})\$[a-zA-Z0-9.\/]{86})$/)
        assert dqMatcher.find()
        assert Sha2Crypt.sha512Crypt("Quote\"Pass123!".getBytes(StandardCharsets.UTF_8), "\$6\$" + dqMatcher.group(2)) == dqMatcher.group(1)
        def sqMatcher = (parsedList[1] =~ /^zstactest:(\$6\$([a-zA-Z0-9.\/]{16})\$[a-zA-Z0-9.\/]{86})$/)
        assert sqMatcher.find()
        assert Sha2Crypt.sha512Crypt("Quote'Pass123!".getBytes(StandardCharsets.UTF_8), "\$6\$" + sqMatcher.group(2)) == sqMatcher.group(1)
        assert parsedList[2].startsWith("testuser:\$6\$")

        destroyVmInstance {
            uuid = vm1.uuid
        }
        destroyVmInstance {
            uuid = vm2.uuid
        }
    }

    private void assertUserdataNotModified(FlatUserdataBackend.ApplyUserdataCmd cmd, String expectedUserdata) {
        assert cmd != null
        assert cmd.userdata.userdataList == [expectedUserdata]
        assert cmd.userdata.metadata.vmHostname == null
    }

    @Override
    void clean() {
        env.delete()
    }
}
