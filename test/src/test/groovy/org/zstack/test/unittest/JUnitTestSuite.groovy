package org.zstack.test.unittest

import org.junit.runner.JUnitCore
import org.junit.runner.Result
import org.junit.runner.RunWith
import org.junit.runner.notification.Failure
import org.junit.runners.Suite
import org.zstack.configuration.OfferingUserConfigUtils
import org.zstack.kvm.memory.MemoryArbiterBoundaryTest
import org.zstack.kvm.memory.MemoryBackendPreparationRulesTest
import org.zstack.kvm.memory.MemoryBatchCursorTest
import org.zstack.kvm.memory.MemoryClusterOperationTest
import org.zstack.kvm.memory.MemoryLifecycleHooksCallbackTest
import org.zstack.kvm.memory.MemoryLifecycleHooksTest
import org.zstack.kvm.memory.MemoryMetricsPresentationTest
import org.zstack.kvm.memory.MemoryObservationRulesTest
import org.zstack.kvm.memory.MemoryOperationExceptionI18nTest
import org.zstack.kvm.memory.MemoryReadApisTest
import org.zstack.kvm.memory.MemoryReadAuthorizationTest
import org.zstack.kvm.memory.MemoryRepositoryRulesTest
import org.zstack.kvm.memory.MemoryResourceCascadeTest
import org.zstack.kvm.memory.MemoryResourceDeletionTest
import org.zstack.kvm.memory.MemoryResultRulesTest
import org.zstack.kvm.memory.MemoryRuntimeSettingsTest
import org.zstack.kvm.memory.MemoryStandardConfigAdapterTest
import org.zstack.kvm.memory.MemorySummaryRulesTest
import org.zstack.kvm.memory.MemoryWritebackBackendInventoryTest
import org.zstack.kvm.memory.MemoryWritebackManagerCallbackTest
import org.zstack.test.unittest.compute.MigrateVmFinalizeCompletionCase
import org.zstack.test.unittest.core.MessageTimeoutContextCase
import org.zstack.test.unittest.network.sdncontroller.ZnsProxyAnsibleDeployerTest
import org.zstack.test.unittest.network.sdncontroller.ZnsProxyInstallerGroovyTest
import org.zstack.test.unittest.network.sdncontroller.ZnsProxyKvmReconnectExtensionTest
import org.zstack.test.unittest.utils.NetworkUtilsCase
import org.zstack.test.unittest.utils.OfferingUserConfigUtilsCase
import org.zstack.test.unittest.utils.SizeUnitUtilsCase
import org.zstack.test.unittest.utils.SystemTagUtilsCase

import java.util.stream.Collectors

/**
 * Created by lining on 2018/3/18.
 */

@RunWith(Suite.class)
@Suite.SuiteClasses([
        MessageTimeoutContextCase.class,
        MigrateVmFinalizeCompletionCase.class,
        NetworkUtilsCase.class,
        ZnsProxyAnsibleDeployerTest.class,
        ZnsProxyInstallerGroovyTest.class,
        ZnsProxyKvmReconnectExtensionTest.class,
        OfferingUserConfigUtilsCase.class,
        SizeUnitUtilsCase.class,
        SystemTagUtilsCase.class,
        MemoryArbiterBoundaryTest.class,
        MemoryBackendPreparationRulesTest.class,
        MemoryBatchCursorTest.class,
        MemoryClusterOperationTest.class,
        MemoryLifecycleHooksCallbackTest.class,
        MemoryLifecycleHooksTest.class,
        MemoryMetricsPresentationTest.class,
        MemoryObservationRulesTest.class,
        MemoryOperationExceptionI18nTest.class,
        MemoryReadApisTest.class,
        MemoryReadAuthorizationTest.class,
        MemoryRepositoryRulesTest.class,
        MemoryResourceCascadeTest.class,
        MemoryResourceDeletionTest.class,
        MemoryResultRulesTest.class,
        MemoryRuntimeSettingsTest.class,
        MemoryStandardConfigAdapterTest.class,
        MemorySummaryRulesTest.class,
        MemoryWritebackBackendInventoryTest.class,
        MemoryWritebackManagerCallbackTest.class
    ])
class JUnitTestSuite {

    static void runAllTestCases() {
        Result result = JUnitCore.runClasses(JUnitTestSuite.class)

        List<Failure> failures = result.getFailures()
        if (!failures.isEmpty()) {
            List<String> errors = failures.stream().map{failure -> failure.toString()}.collect(Collectors.toList())
            assert false : "JUnit test fail, " +  errors.toString()
        }
    }
}
