package org.zstack.test.integration.ldap.forcase

import com.unboundid.ldap.sdk.LDAPInterface
import com.unboundid.ldap.sdk.SearchResult
import com.unboundid.ldap.sdk.SearchScope
import org.zstack.sdk.AddLdapServerAction
import org.zstack.sdk.LdapServerInventory
import org.zstack.test.integration.ldap.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.test.integration.ldap.LdapTest
import org.zstack.testlib.Test

/**
 * Created by Administrator on 2017-03-22.
 */


//base on TestLdapConn
class LdapConnCase extends SubCase {
    EnvSpec env


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

    void testLdapConn(){
        final LDAPInterface ldapConnection = LdapTest.embeddedLdapRule.ldapConnection()
        final SearchResult searchResult = ldapConnection.search(LdapTest.DOMAIN_DSN, SearchScope.SUB, "(objectClass=person)")
        assert searchResult.getEntryCount() == 3


        def result = addLdapServer {
            name = "ldap0"
            description = "test-ldap0"
            base = LdapTest.DOMAIN_DSN
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
                base : LdapTest.DOMAIN_DSN,
                url : "ldap://localhost:1888",
                username : "",
                password : "",
                encryption : "None",
                systemTags : ["ephemeral::validationOnly"],
                sessionId : Test.currentEnvSpec.session.uuid
        )
        AddLdapServerAction.Result addLdapResult = addLdapServerAction.call()
        assert null == addLdapResult.error
        assert null == addLdapResult.value.inventory

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
