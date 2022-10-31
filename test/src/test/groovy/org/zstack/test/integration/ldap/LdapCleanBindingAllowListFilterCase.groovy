package org.zstack.test.integration.ldap

import org.junit.ClassRule
import org.zapodot.junit.ldap.EmbeddedLdapRule
import org.zapodot.junit.ldap.EmbeddedLdapRuleBuilder
import org.zstack.ldap.LdapSystemTags
import org.zstack.sdk.LdapServerInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * @author shenjin
 * @date 2022/10/24 16:45
 */
class LdapCleanBindingAllowListFilterCase extends SubCase{
    EnvSpec env

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testAddLdapWithoutFilter()
            testUpdateFiler()
            testDeleteLdapServer()
            testAddLdapWithFilter()
            testUpdateFiler()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testAddLdapWithoutFilter(){
        LdapServerInventory ldapServerInventory = addLdapServer {
            name = "ldap0"
            description = "test-ldap0"
            base = ZStackTest.DOMAIN_DSN
            url = "ldap://localhost:1888"
            username = ""
            password = ""
            encryption = "None"
        } as LdapServerInventory

        assert null ==  LdapSystemTags.LDAP_ALLOW_LIST_FILTER.getTag(ldapServerInventory.uuid)
    }

    void testUpdateFiler(){
        String filter = "(age=3)"

        LdapServerInventory ldapServerInventory = queryLdapServer {
        }[0]

        updateLdapServer {
            ldapServerUuid = ldapServerInventory.uuid
            systemTags = [LdapSystemTags.LDAP_ALLOW_LIST_FILTER.instantiateTag([(LdapSystemTags.LDAP_ALLOW_LIST_FILTER_TOKEN): filter])]
        }

        assert filter == LdapSystemTags.LDAP_ALLOW_LIST_FILTER.getTokenByResourceUuid(ldapServerInventory.uuid, LdapSystemTags.LDAP_ALLOW_LIST_FILTER_TOKEN)
    }

    void testAddLdapWithFilter(){
        String filter = "(cn=Micha Kops)"

        LdapServerInventory newLdapServerInventory = addLdapServer {
            name = "ldap1"
            description = "test-ldap0"
            base = ZStackTest.DOMAIN_DSN
            url = "ldap://localhost:1888"
            username = ""
            password = ""
            encryption = "None"
            systemTags = [LdapSystemTags.LDAP_ALLOW_LIST_FILTER.instantiateTag([(LdapSystemTags.LDAP_ALLOW_LIST_FILTER_TOKEN): filter])]
        } as LdapServerInventory

        assert filter == LdapSystemTags.LDAP_ALLOW_LIST_FILTER.getTokenByResourceUuid(newLdapServerInventory.uuid, LdapSystemTags.LDAP_ALLOW_LIST_FILTER_TOKEN)
    }

    void testDeleteLdapServer(){
        LdapServerInventory ldapServerInventory = queryLdapServer {
        }[0]

        deleteLdapServer {
            uuid = ldapServerInventory.uuid
        }

        assert null ==  LdapSystemTags.LDAP_ALLOW_LIST_FILTER.getTag(ldapServerInventory.uuid)
    }
}
