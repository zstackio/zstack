package org.zstack.test.unittest

import com.jcraft.jsch.SshExecutionTimeoutCase
import org.junit.runner.JUnitCore
import org.junit.runner.Result
import org.junit.runner.RunWith
import org.junit.runner.notification.Failure
import org.junit.runners.Suite
import org.zstack.configuration.OfferingUserConfigUtils
import org.zstack.core.ansible.AnsibleAgentRestartCase
import org.zstack.storage.ceph.backup.CephUploadCancelRecoveryCase
import org.zstack.test.longjob.TestLongJobInventorySecurity
import org.zstack.test.unittest.compute.MigrateVmFinalizeCompletionCase
import org.zstack.test.unittest.compute.VmDataVolumeTemplateQuotaCase
import org.zstack.test.unittest.core.MessageTimeoutContextCase
import org.zstack.test.unittest.kvm.KvmAgentRestartGlobalConfigCase
import org.zstack.test.unittest.kvm.KVMHostSshCredentialTimeoutCase
import org.zstack.test.unittest.kvm.KVMHostCpuModelCheckCase
import org.zstack.test.unittest.kvm.HypervisorMetadataTimeoutCase
import org.zstack.test.unittest.rest.TestDocTemplateLocationOverrideCase
import org.zstack.test.unittest.network.sdncontroller.ZnsProxyAnsibleDeployerTest
import org.zstack.test.unittest.network.sdncontroller.ZnsProxyInstallerGroovyTest
import org.zstack.test.unittest.network.sdncontroller.ZnsProxyKvmReconnectExtensionTest
import org.zstack.test.unittest.utils.NetworkUtilsCase
import org.zstack.test.unittest.utils.OfferingUserConfigUtilsCase
import org.zstack.test.unittest.utils.SizeUnitUtilsCase
import org.zstack.test.unittest.utils.ShellTimeoutCase
import org.zstack.test.unittest.utils.SystemTagUtilsCase
import org.zstack.utils.ssh.SshExecutionDeadlineCase
import org.zstack.test.unittest.vfs.VFSMetadataReadWriteTest

import java.util.stream.Collectors

/**
 * Created by lining on 2018/3/18.
 */

@RunWith(Suite.class)
@Suite.SuiteClasses([
        AnsibleAgentRestartCase.class,
        KvmAgentRestartGlobalConfigCase.class,
        SshExecutionTimeoutCase.class,
        SshExecutionDeadlineCase.class,
        MessageTimeoutContextCase.class,
        KVMHostSshCredentialTimeoutCase.class,
        KVMHostCpuModelCheckCase.class,
        HypervisorMetadataTimeoutCase.class,
        ShellTimeoutCase.class,
        TestDocTemplateLocationOverrideCase.class,
        MigrateVmFinalizeCompletionCase.class,
        VmDataVolumeTemplateQuotaCase.class,
        CephUploadCancelRecoveryCase.class,
        NetworkUtilsCase.class,
        ZnsProxyAnsibleDeployerTest.class,
        ZnsProxyInstallerGroovyTest.class,
        ZnsProxyKvmReconnectExtensionTest.class,
        OfferingUserConfigUtilsCase.class,
        SizeUnitUtilsCase.class,
        SystemTagUtilsCase.class,
        VFSMetadataReadWriteTest.class,
        TestLongJobInventorySecurity.class
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
