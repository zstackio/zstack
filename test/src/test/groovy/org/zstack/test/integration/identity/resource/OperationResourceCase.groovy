package org.zstack.test.integration.identity.resource

import org.zstack.core.db.DatabaseFacade
import org.zstack.header.identity.AccountConstant
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.SessionInventory
import org.zstack.sdk.UpdateInstanceOfferingAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.identity.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil
/**
 * Created by camile on 2017/6/11.
 */
class OperationResourceCase extends SubCase {
    EnvSpec envSpec
    AccountInventory accountInventory

    @Override
    void clean() {
        envSpec.delete()
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        envSpec = Env.oneVmBasicEnv()
    }

    void testChangeOwnerWhenVMStarting() {
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        VmInstanceInventory vm = envSpec.inventoryByName("vm")
        accountInventory = createAccount {
            name = "test"
            password = "password"
        }
        VmInstanceVO vmVo = dbf.findByUuid(vm.uuid, VmInstanceVO.class)
        vmVo.setState(VmInstanceState.Starting)
        vmVo = dbf.updateAndRefresh(vmVo)

        changeResourceOwner{
            accountUuid = accountInventory.uuid
            resourceUuid = vmVo.uuid
        }

        accountInventory = createAccount {
            name = "test2"
            password = "password"
        }

        vmVo.setState(VmInstanceState.Paused)
        vmVo = dbf.updateAndRefresh(vmVo)
        changeResourceOwner {
            accountUuid = accountInventory.uuid
            resourceUuid = vmVo.uuid
        }
    }

    void testShareResourceAgain(){
        logInByAccount {
            accountName = AccountConstant.INITIAL_SYSTEM_ADMIN_NAME
            password = AccountConstant.INITIAL_SYSTEM_ADMIN_PASSWORD
        }

        InstanceOfferingInventory instanceOfferingInventory = envSpec.inventoryByName("instanceOffering")
        shareResource {
            resourceUuids = [instanceOfferingInventory.uuid]
            accountUuids = [accountInventory.uuid]
        }

        revokeResourceSharing{
            resourceUuids = [instanceOfferingInventory.uuid]
            accountUuids = [accountInventory.uuid]
        }
    }

    void testSharedResourceOperator() {
        def instanceOfferingInventory = envSpec.inventoryByName("instanceOffering") as InstanceOfferingInventory
        def image = envSpec.inventoryByName("image1") as ImageInventory
        shareResource {
            resourceUuids = [instanceOfferingInventory.uuid, image.uuid]
            accountUuids = [accountInventory.uuid]
        }
        logOut {
            sessionUuid = adminSession()
        }

        def session = logInByAccount {
            accountName = "test"
            password = "password"
        } as SessionInventory
        def action = new UpdateInstanceOfferingAction()
        action.sessionId = session.uuid
        action.uuid = instanceOfferingInventory.uuid
        action.name = "updated"
        assert action.call().error != null
    }

    void testResourceAccessAndReferenceErrorCodes() {
        def admin = logInByAccount {
            accountName = AccountConstant.INITIAL_SYSTEM_ADMIN_NAME
            password = AccountConstant.INITIAL_SYSTEM_ADMIN_PASSWORD
        } as SessionInventory
        def offering = envSpec.inventoryByName("instanceOffering") as InstanceOfferingInventory
        def image = envSpec.inventoryByName("image1") as ImageInventory
        def l3 = envSpec.inventoryByName("l3") as L3NetworkInventory
        def pubL3 = envSpec.inventoryByName("pubL3") as L3NetworkInventory
        def rbacTenant = createAccount {
            name = "rbac-error-code-tenant"
            password = "password"
            sessionId = admin.uuid
        } as AccountInventory

        try {
            shareResource {
                resourceUuids = [offering.uuid, image.uuid]
                accountUuids = [rbacTenant.uuid]
                sessionId = admin.uuid
            }
            def tenantSession = logInByAccount {
                accountName = rbacTenant.name
                password = "password"
            } as SessionInventory

            def directDeny = new CreateVmInstanceAction()
            directDeny.name = "91840-direct-param-deny"
            directDeny.imageUuid = image.uuid
            directDeny.instanceOfferingUuid = offering.uuid
            directDeny.l3NetworkUuids = [l3.uuid]
            directDeny.sessionId = tenantSession.uuid
            def directRet = directDeny.call()
            assert directRet.error != null
            assert directRet.error.details.contains(l3.uuid)
            assert directRet.error.globalErrorCode == "ORG_ZSTACK_IDENTITY_RBAC_10009"

            shareResource {
                resourceUuids = [l3.uuid]
                accountUuids = [rbacTenant.uuid]
                sessionId = admin.uuid
            }

            def nestedDeny = new CreateVmInstanceAction()
            nestedDeny.name = "91840-nested-ref-deny"
            nestedDeny.imageUuid = image.uuid
            nestedDeny.instanceOfferingUuid = offering.uuid
            nestedDeny.l3NetworkUuids = [l3.uuid]
            nestedDeny.vmNicParams = JSONObjectUtil.toJsonString([[l3NetworkUuid: pubL3.uuid, isDefaultNic: true]])
            nestedDeny.sessionId = tenantSession.uuid
            def nestedRet = nestedDeny.call()
            assert nestedRet.error != null
            assert nestedRet.error.details.contains(pubL3.uuid)
            assert nestedRet.error.globalErrorCode == "ORG_ZSTACK_IDENTITY_RBAC_10014"
        } finally {
            deleteAccount {
                uuid = rbacTenant.uuid
                sessionId = admin.uuid
            }
        }
    }

    @Override
    void test() {
        envSpec.create {
            testChangeOwnerWhenVMStarting()
            testShareResourceAgain()
            testSharedResourceOperator()
            testResourceAccessAndReferenceErrorCodes()
        }
    }
}
