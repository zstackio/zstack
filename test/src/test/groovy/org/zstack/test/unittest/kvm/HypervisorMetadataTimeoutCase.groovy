package org.zstack.test.unittest.kvm

import org.junit.Test
import org.mockito.MockedStatic
import org.zstack.kvm.hypervisor.HypervisorMetadataCollector.HypervisorMetadataDefinition
import org.zstack.kvm.hypervisor.HypervisorMetadataCollectorImpl
import org.zstack.utils.ShellResult
import org.zstack.utils.ShellUtils

import java.nio.file.Path
import java.nio.file.Paths

import static org.mockito.Mockito.mockStatic

class HypervisorMetadataTimeoutCase {
    @Test
    void collectsMetadataWithDeadline() {
        HypervisorMetadataDefinition definition = definition('healthy')
        MockedStatic<ShellUtils> shell = mockStatic(ShellUtils.class)
        try {
            shell.when({ ShellUtils.runAndReturn(command(definition), true, 60L) })
                    .thenReturn(metadata())

            assert collector([definition]).collect() == [definition]
            assert definition.version == '6.2.0-1'
            assert definition.osReleaseVersion == 'helix 8.4'
            shell.verify({ ShellUtils.runAndReturn(command(definition), true, 60L) })
        } finally {
            shell.close()
        }
    }

    @Test
    void skipsTimedOutScriptAndContinuesCollection() {
        HypervisorMetadataDefinition blocked = definition('blocked')
        HypervisorMetadataDefinition healthy = definition('healthy')
        MockedStatic<ShellUtils> shell = mockStatic(ShellUtils.class)
        try {
            shell.when({ ShellUtils.runAndReturn(command(blocked), true, 60L) })
                    .thenThrow(new ShellUtils.ShellTimeoutException(command(blocked), 60))
            shell.when({ ShellUtils.runAndReturn(command(healthy), true, 60L) })
                    .thenReturn(metadata())

            assert collector([blocked, healthy]).collect() == [healthy]
            assert blocked.version == null
            assert healthy.version == '6.2.0-1'
            assert !Thread.currentThread().isInterrupted()
            shell.verify({ ShellUtils.runAndReturn(command(blocked), true, 60L) })
        } finally {
            shell.close()
        }
    }

    @Test
    void preservesNonzeroExitHandling() {
        HypervisorMetadataDefinition failed = definition('failed')
        MockedStatic<ShellUtils> shell = mockStatic(ShellUtils.class)
        try {
            shell.when({ ShellUtils.runAndReturn(command(failed), true, 60L) })
                    .thenReturn(new ShellResult(retCode: 1, stderr: 'rpm query failed', stdout: ''))

            assert collector([failed]).collect().isEmpty()
            shell.verify({ ShellUtils.runAndReturn(command(failed), true, 60L) })
        } finally {
            shell.close()
        }
    }

    private static HypervisorMetadataCollectorImpl collector(List<HypervisorMetadataDefinition> definitions) {
        return new HypervisorMetadataCollectorImpl() {
            @Override
            protected List<HypervisorMetadataDefinition> scanFolder(Path root) {
                return new ArrayList<>(definitions)
            }
        }
    }

    private static HypervisorMetadataDefinition definition(String name) {
        return new HypervisorMetadataDefinition() {
            @Override
            Path virtualizerScriptPath() {
                return Paths.get('/test', name, 'virtualizer-info.sh')
            }

            @Override
            boolean isValid() {
                return true
            }
        }
    }

    private static String command(HypervisorMetadataDefinition definition) {
        return '/bin/bash ' + definition.virtualizerScriptPath()
    }

    private static ShellResult metadata() {
        return new ShellResult(retCode: 0, stdout: '''qemu-kvm.version: 6.2.0-1
platform.distname: helix
platform.version: 8.4
''')
    }
}
