package org.zstack.test.integration.core.config

import org.zstack.compute.host.HostGlobalConfig
import org.zstack.core.config.GlobalConfigFacadeImpl
import org.zstack.core.config.GlobalConfigVO
import org.zstack.core.db.DatabaseFacade
import org.zstack.image.ImageGlobalConfig
import org.zstack.kvm.KVMGlobalConfig
import org.zstack.sdk.GlobalConfigInventory
import org.zstack.sdk.UpdateGlobalConfigAction
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by miao on 17-5-4.
 */
class GlobalConfigCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            zone {
                name = "zone"
            }
        }
    }

    @Override
    void test() {
        env.create {
            testTolerateDatabaseDirtyData()
            testUpdateIntegerConfigWithFloatValue()
            testFloatPointNumberTolerance()
            testBooleanValidator()
            testBorderValue()
            testImageGlobalConfig()
        }
    }

    void testFloatPointNumberTolerance() {
        // test if the global config can convert float to int
        KVMGlobalConfig.VM_MIGRATION_QUANTITY.@value = 1.0
        assert KVMGlobalConfig.VM_MIGRATION_QUANTITY.value(int.class) == 1
    }

    void testUpdateIntegerConfigWithFloatValue() {
        expect(AssertionError.class) {
            updateGlobalConfig {
                category = KVMGlobalConfig.VM_MIGRATION_QUANTITY.category
                name = KVMGlobalConfig.VM_MIGRATION_QUANTITY.name
                value = "1.0"
            }
        }
    }

    void testTolerateDatabaseDirtyData() {
        // make dirty data in database
        GlobalConfigVO vo = KVMGlobalConfig.VM_MIGRATION_QUANTITY.reload()
        vo.setValue("1.0")
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        dbf.update(vo)

        GlobalConfigFacadeImpl gcf = bean(GlobalConfigFacadeImpl.class)
        gcf.start()

        assert KVMGlobalConfig.VM_MIGRATION_QUANTITY.value(int.class) == 1
    }

    void testBooleanValidator() {
        updateGlobalConfig {
            category = GlobalConfigForTest.CATEGORY
            name = GlobalConfigForTest.TEST_GlobalConfig2_Boolean.name
            value = "TRUE"
            sessionId = adminSession()
        }


        UpdateGlobalConfigAction action = new UpdateGlobalConfigAction()
        action.category = GlobalConfigForTest.CATEGORY
        action.name = GlobalConfigForTest.TEST_GlobalConfig2_Boolean.name
        action.value = "miao"
        action.sessionId = adminSession()
        UpdateGlobalConfigAction.Result result = action.call()
        assert result.error != null
    }

    void testBorderValue() {
        def action1 = new UpdateGlobalConfigAction() as UpdateGlobalConfigAction
        action1.category = GlobalConfigForTest.CATEGORY
        action1.name = GlobalConfigForTest.TEST_GLOBALCONFIG_BORDER.name
        action1.value = 0
        action1.sessionId = adminSession()
        UpdateGlobalConfigAction.Result result1 = action1.call()

        assert result1.error == null

        def action2 = new UpdateGlobalConfigAction() as UpdateGlobalConfigAction
        action2.category = GlobalConfigForTest.CATEGORY
        action2.name = GlobalConfigForTest.TEST_GLOBALCONFIG_BORDER.name
        action2.value = 100
        action2.sessionId = adminSession()
        UpdateGlobalConfigAction.Result result2 = action2.call()

        assert result2.error == null
    }

    void testImageGlobalConfig(){
        updateGlobalConfig {
            category = ImageGlobalConfig.CATEGORY
            name = ImageGlobalConfig.EXPUNGE_INTERVAL.name
            value = 1800
        }

        expect(AssertionError.class) {
            updateGlobalConfig {
                category = ImageGlobalConfig.CATEGORY
                name = ImageGlobalConfig.EXPUNGE_INTERVAL.name
                value = 0
            }
        }
    }
}
