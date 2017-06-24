package org.zstack.test.integration.storage.primary.local

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.SQL
import org.zstack.header.identity.AccountConstant
import org.zstack.header.identity.SharedResourceVO
import org.zstack.header.vm.VmInstanceConstant
import org.zstack.header.vm.VmInstanceVO
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.GetVmCapabilitiesResult
import org.zstack.sdk.HostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.storage.primary.local.LocalStoragePrimaryStorageGlobalConfig
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by miao on 17-5-7.
 */
class LiveMigrateVmCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testLiveMigrateVmWithDataVolume()
        }
    }

    void testLiveMigrateVmWithDataVolume() {
        dbf = bean(DatabaseFacade.class)
        VmInstanceInventory vm1 = (VmInstanceInventory) env.inventoryByName("vm")
        def invs = queryHost {
        } as List<HostInventory>
        def targetHostUuid = invs.find { i -> i.uuid != vm1.getHostUuid() }.getUuid()

        // default false
        GetVmCapabilitiesResult capRes = getVmCapabilities {
            uuid = vm1.getUuid()
        } as GetVmCapabilitiesResult
        assert !capRes.capabilities.get(VmInstanceConstant.Capability.LiveMigration.toString()) as Boolean

        // set true
        LocalStoragePrimaryStorageGlobalConfig.ALLOW_LIVE_MIGRATION.updateValue(Boolean.TRUE.toString())
        GetVmCapabilitiesResult capRes2 = getVmCapabilities {
            uuid = vm1.getUuid()
        } as GetVmCapabilitiesResult
        assert capRes2.capabilities.get(VmInstanceConstant.Capability.LiveMigration.toString()) as Boolean

        // record create empty volume cmd
        LocalStorageKvmBackend.CreateEmptyVolumeCmd cmd = null
        env.afterSimulator(LocalStorageKvmBackend.CREATE_EMPTY_VOLUME_PATH) {
            LocalStorageKvmBackend.CreateEmptyVolumeRsp rsp, HttpEntity<String> e ->
                cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.CreateEmptyVolumeCmd.class)
                rsp.success = true
                return rsp
        }

        // share resource, change owner
        AccountInventory account = (AccountInventory) createAccount {
            name = "test"
            password = "test"
        }
        shareResource {
            resourceUuids = Arrays.asList(vm1.getDefaultL3NetworkUuid())
            toPublic = true
        }
        changeResourceOwner {
            resourceUuid = vm1.getUuid()
            accountUuid = account.getUuid()
        }

        // migrate vm
        migrateVm {
            vmInstanceUuid = vm1.getUuid()
            hostUuid = targetHostUuid
        }

        // make sure migration success
        retryInSecs {
            VmInstanceVO vmInstanceVO = dbf.findByUuid(vm1.getUuid(), VmInstanceVO.class)
            assert vmInstanceVO.hostUuid == targetHostUuid
        }

        // make sure path keep same
        retryInSecs {
            assert cmd.installUrl.contains(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)
            assert !cmd.installUrl.contains(account.uuid)
        }

        SQL.New(SharedResourceVO.class).hardDelete()
    }
}
