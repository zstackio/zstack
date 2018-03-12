package org.zstack.test.integration.identity

import org.zstack.header.errorcode.OperationFailureException
import org.zstack.header.identity.APICreateAccountMsg
import org.zstack.header.identity.SessionVO
import org.zstack.header.identity.SuppressCredentialCheck
import org.zstack.header.identity.rbac.PolicyMatcher
import org.zstack.header.identity.rbac.RBACInfo
import org.zstack.header.message.APIMessage
import org.zstack.header.rest.RestRequest

import org.zstack.identity.rbac.RBACAPIRequestChecker
import org.zstack.sdk.SessionInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.BeanUtils

import java.lang.reflect.Modifier

class RBACAPIRequestCheckerCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            account {
                name = "test"
                password = "password"
            }
        }
    }

    void doAdminCheck(Class apiclz, org.zstack.header.identity.SessionInventory session) {
        APIMessage msg = apiclz.newInstance()
        msg.session = session
        RBACAPIRequestChecker checker = new RBACAPIRequestChecker()
        logger.debug("... checking API[${apiclz.name}] for admin account")
        checker.check(msg)
    }

    void doNormalAccountCheck(Class apiclz, org.zstack.header.identity.SessionInventory session, boolean adminOnly) {
        APIMessage msg = apiclz.newInstance()
        msg.session = session
        RBACAPIRequestChecker checker = new RBACAPIRequestChecker()
        logger.debug("... checking API[${apiclz.name}, admin-only: ${adminOnly}] for normal accounts")

        if (adminOnly) {
            expect(OperationFailureException.class) {
                checker.check(msg)
            }
        } else {
            checker.check(msg)
        }
    }

    void testAPIPermissions() {
        SessionInventory normal = logInByAccount {
            accountName = "test"
            password = "password"
        }

        SessionVO svo = dbFindByUuid(normal.uuid, SessionVO.class)
        org.zstack.header.identity.SessionInventory n = org.zstack.header.identity.SessionInventory.valueOf(svo)

        SessionInventory admin = loginAsAdmin()
        svo = dbFindByUuid(admin.uuid, SessionVO.class)
        org.zstack.header.identity.SessionInventory a = org.zstack.header.identity.SessionInventory.valueOf(svo)

        def apis = BeanUtils.reflections.getSubTypesOf(APIMessage.class).findAll {
            !Modifier.isAbstract(it.modifiers) && !it.isAnnotationPresent(SuppressCredentialCheck.class) \
            && it.isAnnotationPresent(RestRequest.class) && !it.isAnnotationPresent(Deprecated.class)
        }

        Set<String> adminOnlyAPIs = []
        RBACInfo.infos.each { info ->
            adminOnlyAPIs.addAll(info.adminOnlyAPIs)
        }

        def matcher = new PolicyMatcher()
        apis.each { apiclz ->
            if (!apiclz.name.startsWith(APICreateAccountMsg.class.package.name)) {
                return
            }

            boolean  adminOnly = adminOnlyAPIs.any { matcher.match(it, apiclz.name) }
            doAdminCheck(apiclz, a)
            doNormalAccountCheck(apiclz, n, adminOnly)
        }
    }

    @Override
    void test() {
        RBACInfo.checkIfAPIsMissingRBACInfo()
        env.create {
            testAPIPermissions()
        }
    }
}
