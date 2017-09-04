package org.zstack.test.integration.ldap.forcase

import com.unboundid.ldap.sdk.LDAPInterface
import com.unboundid.ldap.sdk.SearchResult
import com.unboundid.ldap.sdk.SearchScope
import org.junit.Rule
import org.zapodot.junit.ldap.EmbeddedLdapRule
import org.zapodot.junit.ldap.EmbeddedLdapRuleBuilder
import org.zstack.sdk.AddLdapServerAction
import org.zstack.sdk.AddLdapServerResult
import org.zstack.sdk.ApiResult
import org.zstack.sdk.LdapServerInventory
import org.zstack.sdk.ZSClient
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.ldap.Env
import org.zstack.test.integration.stabilisation.StabilityTestCase
import org.zstack.test.integration.stabilisation.TestCaseStabilityTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test

/**
 * Created by Administrator on 2017-03-22.
 */

//base on TestLdapConn
class LdapConnCase extends SubCase {
    EnvSpec env

    public static String DOMAIN_DSN = "dc=example,dc=com"

    @Rule
    public static EmbeddedLdapRule embeddedLdapRule = EmbeddedLdapRuleBuilder.newInstance().bindingToPort(1888).
            usingDomainDsn(DOMAIN_DSN).importingLdifs("users-import.ldif").build()


    @Override
    void setup() {
        spring {
            kvm()
            localStorage()
            sftpBackupStorage()
            include("LdapManagerImpl.xml")
        }
    }

    @Override
    void environment() {
        env = Env.localStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testLdapConn()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    LDAPInterface getLdapConn(){
        try{
            return embeddedLdapRule.ldapConnection()
        }catch (Exception e){
        }

        try {
            return ZStackTest.embeddedLdapRule.ldapConnection()
        }catch (Exception e){
        }

        try {
            return StabilityTestCase.embeddedLdapRule.ldapConnection()
        }catch (Exception e){
        }

        try {
            return TestCaseStabilityTest.embeddedLdapRule.ldapConnection()
        }catch (Exception e){
        }
    }

    void testLdapConn(){
        LDAPInterface ldapConnection = this.getLdapConn()
        final SearchResult searchResult = ldapConnection.search(ZStackTest.DOMAIN_DSN, SearchScope.SUB, "(objectClass=person)")
        assert searchResult.getEntryCount() == 3

        def result = addLdapServer {
            name = "ldap0"
            description = "test-ldap0"
            base = ZStackTest.DOMAIN_DSN
            url = "ldap://localhost:1888"
            username = ""
            password = ""
            encryption = "None"
            sessionId = currentEnvSpec.session.uuid
        } as LdapServerInventory
        String LdapUuid = result.uuid

        AddLdapServerAction addLdapServerAction = new AddLdapServerAction(
                name : "ldap0",
                description : "test-ldap0",
                base : ZStackTest.DOMAIN_DSN,
                url : "ldap://localhost:1888",
                username : "",
                password : "",
                encryption : "None",
                systemTags : ["ephemeral::validationOnly"],
                sessionId : Test.currentEnvSpec.session.uuid
        )
        ApiResult res = ZSClient.call(addLdapServerAction)
        assert res.error == null
        assert null == res.getResult(AddLdapServerResult.class).inventory
        /*
        AddLdapServerAction.Result addLdapResult = addLdapServerAction.call()
        assert null == addLdapResult.error
        assert null == addLdapResult.value.inventory
        */

        deleteLdapServer {
            uuid = LdapUuid
            sessionId = currentEnvSpec.session.uuid
        }
        result = addLdapServer {
            name = "ldap1"
            description = "test-ldap1"
            base = "dc=mevoco,dc=com"
            url = "ldap://172.20.11.200:389"
            username = "uid=admin,cn=users,cn=accounts,dc=mevoco,dc=com"
            password = "password"
            encryption = "TLS"
            sessionId = currentEnvSpec.session.uuid
        } as LdapServerInventory
        LdapUuid = result.uuid
        deleteLdapServer {
            uuid = LdapUuid
            sessionId = currentEnvSpec.session.uuid
        }

        result = addLdapServer {
            name = "ldap2"
            description = "test-ldap2"
            base = "dc=mevoco,dc=com"
            url = "ldap://172.20.11.200:389"
            username = "uid=admin,cn=users,cn=accounts,dc=mevoco,dc=com"
            password = "password"
            encryption = "None"
            sessionId = currentEnvSpec.session.uuid
        } as LdapServerInventory
        LdapUuid = result.uuid
        deleteLdapServer {
            uuid = LdapUuid
            sessionId = currentEnvSpec.session.uuid
        }

        result = addLdapServer {
            name = "ldap3"
            description = "test-ldap3"
            base = "dc=learnitguide,dc=net"
            url = "ldap://172.20.12.176:389"
            username = "cn=Manager,dc=learnitguide,dc=net"
            password = "password"
            encryption = "None"
            sessionId = currentEnvSpec.session.uuid
        } as LdapServerInventory
        LdapUuid = result.uuid
        deleteLdapServer {
            uuid = LdapUuid
            sessionId = currentEnvSpec.session.uuid
        }


        result = addLdapServer {
            name = "ldap4"
            description = "test-ldap4"
            base = "dc=mevoco,dc=com"
            url = "ldap://172.20.11.200:389"
            username = "uid=admin,cn=users,cn=accounts,dc=mevoco,dc=com"
            password = "password"
            encryption = "None"
            sessionId = currentEnvSpec.session.uuid
        } as LdapServerInventory
        LdapUuid = result.uuid
        deleteLdapServer {
            uuid = LdapUuid
            sessionId = currentEnvSpec.session.uuid
        }
    }
}
