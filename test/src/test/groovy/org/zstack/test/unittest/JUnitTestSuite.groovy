package org.zstack.test.unittest

import org.junit.runner.JUnitCore
import org.junit.runner.Result
import org.junit.runner.RunWith
import org.junit.runner.notification.Failure
import org.junit.runners.Suite
import org.zstack.configuration.OfferingUserConfigUtils
import org.zstack.test.unittest.core.MessageTimeoutContextCase
import org.zstack.test.unittest.network.sdncontroller.ZnsProxyAnsibleDeployerTest
import org.zstack.test.unittest.network.sdncontroller.ZnsProxyInstallerGroovyTest
import org.zstack.test.unittest.network.sdncontroller.ZnsProxyKvmReconnectExtensionTest
import org.zstack.test.unittest.utils.NetworkUtilsCase
import org.zstack.test.unittest.utils.OfferingUserConfigUtilsCase
import org.zstack.test.unittest.utils.SizeUnitUtilsCase
import org.zstack.test.unittest.vm.VmInstanceStateCase

import java.util.stream.Collectors

/**
 * Created by lining on 2018/3/18.
 */

@RunWith(Suite.class)
@Suite.SuiteClasses([
        MessageTimeoutContextCase.class,
        NetworkUtilsCase.class,
        ZnsProxyAnsibleDeployerTest.class,
        ZnsProxyInstallerGroovyTest.class,
        ZnsProxyKvmReconnectExtensionTest.class,
        OfferingUserConfigUtilsCase.class,
        SizeUnitUtilsCase.class,
        VmInstanceStateCase.class
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
