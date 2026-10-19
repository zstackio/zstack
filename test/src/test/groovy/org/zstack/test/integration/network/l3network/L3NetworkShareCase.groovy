package org.zstack.test.integration.network.l3network

import org.zstack.header.network.l3.L3NetworkConstant
import org.zstack.network.l3.L3NetworkGlobalConfig
import org.zstack.sdk.DeleteResourceConfigAction
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.IpRangeInventory
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.SessionInventory
import org.zstack.sdk.UpdateResourceConfigAction
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase


import static java.util.Arrays.asList

/**
 * Created by Boce on 2021-09-03.
 * Shared L3 networks expose IP ranges and allocation configuration without granting config write access.
 */
class L3NetworkShareCase extends SubCase{
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
        useSpring(KvmTest.springSpec)
    }
    @Override
    void environment() {
        env = Env.OneIpL3Network()
    }

    @Override
    void test() {
        env.create {
            testL3NetworkShareResourceAndIpRange()
        }
    }

    void testL3NetworkShareResourceAndIpRange() {
        L3NetworkInventory l3_1 = env.inventoryByName("l3-1")
        updateResourceConfig {
            category = L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY.category
            name = L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY.name
            resourceUuid = l3_1.uuid
            value = L3NetworkConstant.FIRST_AVAILABLE_IP_ALLOCATOR_STRATEGY
        }
        def queryConfig = { String networkUuid, String session ->
            zQLQuery {
                zql = "query ResourceConfig where resourceUuid='${networkUuid}' and " +
                        "category='${L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY.category}' and " +
                        "name='${L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY.name}'"
                sessionId = session
            }.results[0].inventories
        }

        addIpRange {
            name = "ipr-3"
            l3NetworkUuid = l3_1.getUuid()
            startIp = "192.168.100.101"
            endIp = "192.168.100.200"
            gateway = "192.168.100.1"
            netmask = "255.255.255.0"
        }

        AccountInventory account = createAccount {
            name = "test"
            password = "password"
        }
        SessionInventory sessionInventory = logInByAccount {
            accountName = "test"
            password = "password"
        }
        createAccount {
            name = "other-account"
            password = "password"
        }
        SessionInventory otherSession = logInByAccount {
            accountName = "other-account"
            password = "password"
        }

        def range = queryIpRange {
            conditions=["l3NetworkUuid=${l3_1.uuid}".toString()]
            sessionId = sessionInventory.uuid
        }[0] as IpRangeInventory
        assert range == null
        assert queryConfig(l3_1.uuid, sessionInventory.uuid).isEmpty() : "unshared configuration must be hidden"

        shareResource {
            resourceUuids = [l3_1.getUuid()]
            accountUuids = [account.uuid]
        }

        range = queryIpRange {
            conditions=["l3NetworkUuid=${l3_1.uuid}".toString()]
            sessionId = sessionInventory.uuid
        }[0] as IpRangeInventory
        assert range.name == "ipr-3"
        assert queryConfig(l3_1.uuid, sessionInventory.uuid)*.value == [L3NetworkConstant.FIRST_AVAILABLE_IP_ALLOCATOR_STRATEGY] : "receiver must read sequential allocation"
        assert queryConfig(l3_1.uuid, otherSession.uuid).isEmpty() : "targeted sharing must exclude other accounts"
        assert queryConfig(env.inventoryByName("l3-2").uuid, sessionInventory.uuid).isEmpty() : "sharing must preserve the requested parent filter"
        def configs = queryResourceConfig {
            conditions = ["resourceUuid=${l3_1.uuid}".toString(), "name=${L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY.name}".toString()]
            sessionId = sessionInventory.uuid
        }
        assert configs*.value == [L3NetworkConstant.FIRST_AVAILABLE_IP_ALLOCATOR_STRATEGY] : "QueryResourceConfig must expose the shared strategy"

        UpdateResourceConfigAction update = new UpdateResourceConfigAction()
        update.category = L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY.category
        update.name = L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY.name
        update.resourceUuid = l3_1.uuid
        update.value = L3NetworkConstant.RANDOM_IP_ALLOCATOR_STRATEGY
        update.sessionId = sessionInventory.uuid
        def updateResult = update.call()
        assert updateResult.error?.globalErrorCode == "ORG_ZSTACK_IDENTITY_RBAC_10007" : "shared configuration update must be denied: actual=${updateResult.error}"

        DeleteResourceConfigAction delete = new DeleteResourceConfigAction()
        delete.category = L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY.category
        delete.name = L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY.name
        delete.resourceUuid = l3_1.uuid
        delete.sessionId = sessionInventory.uuid
        def deleteResult = delete.call()
        assert deleteResult.error?.globalErrorCode == "ORG_ZSTACK_IDENTITY_RBAC_10007" : "shared configuration deletion must be denied: actual=${deleteResult.error}"
        assert getResourceConfig {
            category = L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY.category
            name = L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY.name
            resourceUuid = l3_1.uuid
        }.value == L3NetworkConstant.FIRST_AVAILABLE_IP_ALLOCATOR_STRATEGY : "denied writes must preserve the administrator's strategy"

        addIpRange {
            name = "ipr-4"
            l3NetworkUuid = l3_1.getUuid()
            startIp = "192.168.100.90"
            endIp = "192.168.100.100"
            gateway = "192.168.100.1"
            netmask = "255.255.255.0"
        }

        range = queryIpRange {
            conditions=["l3NetworkUuid=${l3_1.uuid}".toString(), "name=ipr-4"]
            sessionId = sessionInventory.uuid
        }[0] as IpRangeInventory
        assert range.name == "ipr-4"

        revokeResourceSharing {
            resourceUuids = [l3_1.uuid]
            accountUuids = [account.uuid]
        }
        assert queryConfig(l3_1.uuid, sessionInventory.uuid).isEmpty() : "revoked configuration must be hidden"
        shareResource {
            resourceUuids = [l3_1.uuid]
            toPublic = true
        }
        assert queryConfig(l3_1.uuid, sessionInventory.uuid)*.value == [L3NetworkConstant.FIRST_AVAILABLE_IP_ALLOCATOR_STRATEGY] : "public sharing must expose the strategy"
        assert queryConfig(l3_1.uuid, otherSession.uuid)*.value == [L3NetworkConstant.FIRST_AVAILABLE_IP_ALLOCATOR_STRATEGY] : "public sharing must include other accounts"
        revokeResourceSharing {
            resourceUuids = [l3_1.uuid]
            all = true
        }
        assert queryConfig(l3_1.uuid, sessionInventory.uuid).isEmpty() : "revoked public configuration must be hidden"
        assert queryConfig(l3_1.uuid, otherSession.uuid).isEmpty() : "revoked public sharing must exclude other accounts"
    }
}
