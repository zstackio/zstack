package org.zstack.test.integration.networkservice.provider.virtualrouter.eip

import org.zstack.header.identity.AccountConstant
import org.zstack.header.identity.IdentityErrors
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.APIAttachEipMsg
import org.zstack.network.service.eip.APIChangeEipStateMsg
import org.zstack.network.service.eip.APICreateEipMsg
import org.zstack.network.service.eip.APIDeleteEipMsg
import org.zstack.network.service.eip.APIDetachEipMsg
import org.zstack.network.service.eip.APIUpdateEipMsg
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.eip.EipStateEvent
import org.zstack.network.service.vip.APICreateVipMsg
import org.zstack.network.service.vip.VipConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.AttachEipAction
import org.zstack.sdk.ChangeEipStateAction
import org.zstack.sdk.CreateEipAction
import org.zstack.sdk.DeleteEipAction
import org.zstack.sdk.DetachEipAction
import org.zstack.sdk.EipInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PolicyInventory
import org.zstack.sdk.SessionInventory
import org.zstack.sdk.UpdateEipAction
import org.zstack.sdk.UserGroupInventory
import org.zstack.sdk.UserInventory
import org.zstack.sdk.VipInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.*
import org.zstack.utils.data.SizeUnit

/**
 * 1. create a user
 * 2. assign permissions of allow of creating/changing/updating/attaching/detaching/deleting eip to the user
 * <p>
 * confirm the user can do those operations
 * <p>
 * 3. assign permissions of deny of creating/changing/updating/attaching/detaching/deleting eip to the user
 * <p>
 * confirm the user cannot do those operations
 * <p>
 * 4. create a user added in a group
 * 5. assign permissions of allow of creating/changing/updating/attaching/detaching/deleting eip to the group
 * <p>
 * confirm the user can do those operations
 * <p>
 * 6. assign permissions of deny of creating/changing/updating/attaching/detaching/deleting eip to the group
 * <p>
 * confirm the user cannot do those operations
 */

/**
 * Created by lining on 2017/3/27.
 */
// base on TestPolicyForEip.java
class PolicyForEipCase extends SubCase {
    EnvSpec env

    L3NetworkInventory l3
    VmInstanceInventory vm
    VmNicInventory nic
    VipInventory newVip
    EipInventory eip
    org.zstack.header.identity.PolicyInventory.Statement s
    SessionInventory testAccountSession
    SessionInventory userSession
    UserInventory user1
    PolicyInventory allow
    PolicyInventory allowvip
    UserGroupInventory userGroup
    PolicyInventory deny

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env {

            account {
                name = "test"
                password = "password"
            }

            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
                useAccount("test")
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image"
                    url  = "http://zstack.org/download/test.qcow2"
                    useAccount("test")
                }

                image {
                    name = "vr"
                    url  = "http://zstack.org/download/vr.qcow2"
                    useAccount("test")
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
                        useAccount("test")
                        service {
                            provider = VirtualRouterConstant.PROVIDER_TYPE
                            types = [NetworkServiceType.SNAT.toString(),
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE
                                     ]
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
                        useAccount("test")
                        ip {
                            startIp = "11.168.100.10"
                            endIp = "11.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.100.1"
                        }
                    }
                }

                attachBackupStorage("sftp")

                eip {
                    name = "eip"
                    useVip("pubL3")
                }

                virtualRouterOffering {
                    name = "vro"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }
            }

            vm {
                name = "vm"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useAccount("test")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testUserPolicyAllow()
            testUserPolicyDeny()
            testUserGroupPolicyAllow()
            testUserGroupPolicyDeny()
        }
    }

    void testUserPolicyAllow(){
        l3 = env.inventoryByName("pubL3")
        vm = env.inventoryByName("vm")
        nic = vm.getVmNics()[0]

        testAccountSession = logInByAccount {
            accountName = "test"
            password = "password"
        }
        user1 = createUser {
            name = "user1"
            password = "password"
            sessionId = testAccountSession.uuid
        }

        s = new org.zstack.header.identity.PolicyInventory.Statement()
        s.setName("allowvip")
        s.setEffect(AccountConstant.StatementEffect.Allow)
        s.addAction(String.format("%s:%s", VipConstant.ACTION_CATEGORY, APICreateVipMsg.class.getSimpleName()))
        allowvip = createPolicy {
            name = "allowvip"
            statements = [s]
            sessionId = testAccountSession.uuid
        }

        s = new org.zstack.header.identity.PolicyInventory.Statement()
        s.setName("allow")
        s.setEffect(AccountConstant.StatementEffect.Allow)
        s.addAction(String.format("%s:%s", EipConstant.ACTION_CATEGORY, APICreateEipMsg.class.getSimpleName()))
        s.addAction(String.format("%s:%s", EipConstant.ACTION_CATEGORY, APIUpdateEipMsg.class.getSimpleName()))
        s.addAction(String.format("%s:%s", EipConstant.ACTION_CATEGORY, APIChangeEipStateMsg.class.getSimpleName()))
        s.addAction(String.format("%s:%s", EipConstant.ACTION_CATEGORY, APIAttachEipMsg.class.getSimpleName()))
        s.addAction(String.format("%s:%s", EipConstant.ACTION_CATEGORY, APIDetachEipMsg.class.getSimpleName()))
        s.addAction(String.format("%s:%s", EipConstant.ACTION_CATEGORY, APIDeleteEipMsg.class.getSimpleName()))
        allow = createPolicy {
            name = "allow"
            statements = [s]
            sessionId = testAccountSession.uuid
        }
        attachPolicyToUser {
            userUuid = user1.uuid
            policyUuid = allowvip.uuid
            sessionId = testAccountSession.uuid
        }
        attachPolicyToUser {
            userUuid = user1.uuid
            policyUuid = allow.uuid
            sessionId = testAccountSession.uuid
        }


        userSession = logInByUser {
            userName = "user1"
            password = "password"
            accountUuid = user1.accountUuid
        }
        newVip =  createVip {
            l3NetworkUuid = l3.uuid
            name = "vip"
            sessionId = userSession.uuid
        }
        eip = createEip {
            name = "eip"
            vmNicUuid = nic.uuid
            vipUuid = newVip.uuid
            sessionId = userSession.uuid
        }

        updateEip {
            uuid = eip.uuid
            sessionId = userSession.uuid
        }
        detachEip {
            uuid = eip.uuid
            sessionId = userSession.uuid
        }
        attachEip {
            eipUuid = eip.uuid
            vmNicUuid = nic.uuid
            sessionId = userSession.uuid
        }
        changeEipState {
            uuid = eip.uuid
            stateEvent = EipStateEvent.disable.name()
            sessionId = userSession.uuid
        }
        deleteEip {
            uuid = eip.uuid
            sessionId = userSession.uuid
        }
    }

    void testUserPolicyDeny(){

        newVip = createVip {
            l3NetworkUuid = l3.uuid
            name = "vip"
            sessionId = userSession.uuid
        }
        eip = createEip {
            name = "eip"
            vmNicUuid = nic.uuid
            vipUuid = newVip.uuid
            sessionId = userSession.uuid
        }
        s = new org.zstack.header.identity.PolicyInventory.Statement()
        s.setName("deny")
        s.setEffect(AccountConstant.StatementEffect.Deny)
        s.addAction(String.format("%s:%s", EipConstant.ACTION_CATEGORY, APICreateEipMsg.class.getSimpleName()))
        s.addAction(String.format("%s:%s", EipConstant.ACTION_CATEGORY, APIUpdateEipMsg.class.getSimpleName()))
        s.addAction(String.format("%s:%s", EipConstant.ACTION_CATEGORY, APIChangeEipStateMsg.class.getSimpleName()))
        s.addAction(String.format("%s:%s", EipConstant.ACTION_CATEGORY, APIAttachEipMsg.class.getSimpleName()))
        s.addAction(String.format("%s:%s", EipConstant.ACTION_CATEGORY, APIDetachEipMsg.class.getSimpleName()))
        s.addAction(String.format("%s:%s", EipConstant.ACTION_CATEGORY, APIDeleteEipMsg.class.getSimpleName()))
        deny = createPolicy {
            name = "deny"
            statements = [s]
            sessionId = testAccountSession.uuid
        }
        detachPolicyFromUser {
            userUuid = user1.uuid
            policyUuid = allow.uuid
            sessionId = testAccountSession.uuid
        }
        attachPolicyToUser {
            userUuid = user1.uuid
            policyUuid = deny.uuid
            sessionId = testAccountSession.uuid
        }

        UpdateEipAction updateEipAction = new UpdateEipAction(
                uuid: eip.uuid,
                sessionId : userSession.uuid
        )
        checkErrorCode(updateEipAction, IdentityErrors.PERMISSION_DENIED.toString())


        DetachEipAction detachEipAction = new DetachEipAction(
                uuid : eip.uuid,
                sessionId : userSession.uuid
        )
        checkErrorCode(detachEipAction, IdentityErrors.PERMISSION_DENIED.toString())

        AttachEipAction attachEipAction = new AttachEipAction(
                eipUuid : eip.uuid,
                vmNicUuid : nic.uuid,
                sessionId : userSession.uuid
        )
        checkErrorCode(attachEipAction, IdentityErrors.PERMISSION_DENIED.toString())

        ChangeEipStateAction changeEipStateAction = new ChangeEipStateAction(
                uuid : eip.uuid,
                stateEvent : EipStateEvent.disable.name(),
                sessionId : userSession.uuid
        )
        checkErrorCode(changeEipStateAction, IdentityErrors.PERMISSION_DENIED.toString())

        DeleteEipAction deleteEipAction = new DeleteEipAction(
                uuid: eip.uuid,
                sessionId : userSession.uuid
        )
        checkErrorCode(deleteEipAction, IdentityErrors.PERMISSION_DENIED.toString())

        newVip = createVip {
            l3NetworkUuid = l3.uuid
            name = "vip"
            sessionId = userSession.uuid
        }
        CreateEipAction createEipAction = new CreateEipAction(
                name : "eip",
                vmNicUuid : nic.uuid,
                vipUuid : newVip.uuid,
                sessionId : userSession.uuid
        )
        checkErrorCode(createEipAction, IdentityErrors.PERMISSION_DENIED.toString())

        deleteEip {
            uuid = eip.uuid
        }

    }

    void testUserGroupPolicyAllow(){
        UserInventory user2 = createUser {
            name = "user2"
            password = "password"
            sessionId = testAccountSession.uuid
        }

        userGroup = createUserGroup {
            name = "group"
            sessionId = testAccountSession.uuid
        }
        addUserToGroup {
            userUuid = user2.uuid
            groupUuid = userGroup.uuid
            sessionId = testAccountSession.uuid
        }

        attachPolicyToUserGroup {
            groupUuid = userGroup.uuid
            policyUuid = allowvip.uuid
            sessionId = testAccountSession.uuid
        }
        attachPolicyToUserGroup {
            groupUuid = userGroup.uuid
            policyUuid = allow.uuid
            sessionId = testAccountSession.uuid
        }

        userSession = logInByUser {
            userName = "user2"
            password = "password"
            accountUuid = user2.accountUuid
        }

        newVip =  createVip {
            l3NetworkUuid = l3.uuid
            name = "vip"
            sessionId = userSession.uuid
        }
        eip = createEip {
            name = "eip"
            vmNicUuid = nic.uuid
            vipUuid = newVip.uuid
            sessionId = userSession.uuid
        }

        updateEip {
            uuid = eip.uuid
            sessionId = userSession.uuid
        }
        detachEip {
            uuid = eip.uuid
            sessionId = userSession.uuid
        }
        attachEip {
            eipUuid = eip.uuid
            vmNicUuid = nic.uuid
            sessionId = userSession.uuid
        }
        changeEipState {
            uuid = eip.uuid
            stateEvent = EipStateEvent.disable.name()
            sessionId = userSession.uuid
        }
        deleteEip {
            uuid = eip.uuid
            sessionId = userSession.uuid
        }
    }

    void testUserGroupPolicyDeny(){
        newVip =  createVip {
            l3NetworkUuid = l3.uuid
            name = "vip"
            sessionId = userSession.uuid
        }
        eip = createEip {
            name = "eip"
            vmNicUuid = nic.uuid
            vipUuid = newVip.uuid
            sessionId = userSession.uuid
        }
        detachPolicyFromUserGroup {
            groupUuid = userGroup.uuid
            policyUuid = allow.uuid
            sessionId = testAccountSession.uuid
        }
        attachPolicyToUserGroup {
            groupUuid = userGroup.uuid
            policyUuid = deny.uuid
            sessionId = testAccountSession.uuid
        }

        UpdateEipAction updateEipAction = new UpdateEipAction(
                uuid: eip.uuid,
                sessionId : userSession.uuid
        )
        checkErrorCode(updateEipAction, IdentityErrors.PERMISSION_DENIED.toString())


        DetachEipAction detachEipAction = new DetachEipAction(
                uuid : eip.uuid,
                sessionId : userSession.uuid
        )
        checkErrorCode(detachEipAction, IdentityErrors.PERMISSION_DENIED.toString())

        AttachEipAction attachEipAction = new AttachEipAction(
                eipUuid : eip.uuid,
                vmNicUuid : nic.uuid,
                sessionId : userSession.uuid
        )
        checkErrorCode(attachEipAction, IdentityErrors.PERMISSION_DENIED.toString())

        ChangeEipStateAction changeEipStateAction = new ChangeEipStateAction(
                uuid : eip.uuid,
                stateEvent : EipStateEvent.disable.name(),
                sessionId : userSession.uuid
        )
        checkErrorCode(changeEipStateAction, IdentityErrors.PERMISSION_DENIED.toString())

        DeleteEipAction deleteEipAction = new DeleteEipAction(
                uuid: eip.uuid,
                sessionId : userSession.uuid
        )
        checkErrorCode(deleteEipAction, IdentityErrors.PERMISSION_DENIED.toString())

        newVip = createVip {
            l3NetworkUuid = l3.uuid
            name = "vip"
            sessionId = userSession.uuid
        }
        CreateEipAction createEipAction = new CreateEipAction(
                name : "eip",
                vmNicUuid : nic.uuid,
                vipUuid : newVip.uuid,
                sessionId : userSession.uuid
        )
        checkErrorCode(createEipAction, IdentityErrors.PERMISSION_DENIED.toString())

        deleteEip {
            uuid = eip.uuid
        }

    }

    void checkErrorCode(action, String errorCode ){
        def result = action.call()
        assert result.error.code == errorCode
    }

}
