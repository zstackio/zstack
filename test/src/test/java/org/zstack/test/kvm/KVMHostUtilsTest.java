package org.zstack.test.kvm;

import org.junit.Assert;
import org.junit.Test;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.kvm.KVMHostUtils;

import java.util.concurrent.TimeUnit;

public class KVMHostUtilsTest {
    @Test
    public void shouldRestartLibvirtdDuringDeploy_initOrRestartLibvirtdTriggers() {
        Assert.assertFalse(KVMHostUtils.shouldRestartLibvirtdDuringDeploy(null, null));
        Assert.assertFalse(KVMHostUtils.shouldRestartLibvirtdDuringDeploy("false", "false"));
        Assert.assertTrue(KVMHostUtils.shouldRestartLibvirtdDuringDeploy("true", "false"));
        Assert.assertTrue(KVMHostUtils.shouldRestartLibvirtdDuringDeploy("false", "true"));
        Assert.assertTrue(KVMHostUtils.shouldRestartLibvirtdDuringDeploy("TrUe", null));
        Assert.assertTrue(KVMHostUtils.shouldRestartLibvirtdDuringDeploy(null, "TrUe"));
    }

    @Test
    public void calculateLibvirtRestartEchoTimeout_keepsDefaultAtThreshold() {
        int oldTimeout = CoreGlobalProperty.REST_FACADE_ECHO_TIMEOUT;
        try {
            CoreGlobalProperty.REST_FACADE_ECHO_TIMEOUT = 60;
            Assert.assertEquals(TimeUnit.SECONDS.toMillis(60),
                    KVMHostUtils.calculateLibvirtRestartEchoTimeoutMillis(100));
        } finally {
            CoreGlobalProperty.REST_FACADE_ECHO_TIMEOUT = oldTimeout;
        }
    }

    @Test
    public void calculateLibvirtRestartEchoTimeout_addsOneSecondAfterThreshold() {
        int oldTimeout = CoreGlobalProperty.REST_FACADE_ECHO_TIMEOUT;
        try {
            CoreGlobalProperty.REST_FACADE_ECHO_TIMEOUT = 60;
            Assert.assertEquals(TimeUnit.SECONDS.toMillis(61),
                    KVMHostUtils.calculateLibvirtRestartEchoTimeoutMillis(101));
        } finally {
            CoreGlobalProperty.REST_FACADE_ECHO_TIMEOUT = oldTimeout;
        }
    }

    @Test
    public void calculateLibvirtRestartEchoTimeout_capsAt180Seconds() {
        int oldTimeout = CoreGlobalProperty.REST_FACADE_ECHO_TIMEOUT;
        try {
            CoreGlobalProperty.REST_FACADE_ECHO_TIMEOUT = 60;
            Assert.assertEquals(TimeUnit.SECONDS.toMillis(180),
                    KVMHostUtils.calculateLibvirtRestartEchoTimeoutMillis(10000));
        } finally {
            CoreGlobalProperty.REST_FACADE_ECHO_TIMEOUT = oldTimeout;
        }
    }

    @Test
    public void calculateLibvirtRestartEchoTimeout_doesNotReduceConfiguredTimeoutAboveCap() {
        int oldTimeout = CoreGlobalProperty.REST_FACADE_ECHO_TIMEOUT;
        try {
            CoreGlobalProperty.REST_FACADE_ECHO_TIMEOUT = 180;
            Assert.assertEquals(TimeUnit.SECONDS.toMillis(180),
                    KVMHostUtils.calculateLibvirtRestartEchoTimeoutMillis(10000));

            CoreGlobalProperty.REST_FACADE_ECHO_TIMEOUT = 300;
            Assert.assertEquals(TimeUnit.SECONDS.toMillis(300),
                    KVMHostUtils.calculateLibvirtRestartEchoTimeoutMillis(10000));
        } finally {
            CoreGlobalProperty.REST_FACADE_ECHO_TIMEOUT = oldTimeout;
        }
    }

    @Test
    public void zstac86349_continueReconnectOnLibvirtSocketMaskSystemdTimeout() {
        ErrorCode error = new ErrorCode();
        error.setDetails("[HOST: 192.168.51.12] ERROR: run shell command: systemctl mask libvirtd.socket libvirtd-ro.socket libvirtd-admin.socket libvirtd-tls.socket libvirtd-tcp.socket failed! stderr: Failed to get properties: Failed to activate service 'org.freedesktop.systemd1': timed out (service_start_timeout=25000ms)");

        Assert.assertTrue(KVMHostUtils.shouldContinueReconnectOnAnsibleFailure(false, error));
        Assert.assertFalse(KVMHostUtils.shouldContinueReconnectOnAnsibleFailure(true, error));
    }

    @Test
    public void zstac86349_doNotContinueReconnectOnOtherAnsibleFailures() {
        ErrorCode error = new ErrorCode();
        error.setDetails("[HOST: 192.168.51.12] ERROR: run shell command: systemctl restart libvirtd failed! stderr: Job for libvirtd.service failed");

        Assert.assertFalse(KVMHostUtils.shouldContinueReconnectOnAnsibleFailure(false, error));
    }
}
