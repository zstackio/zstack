package org.zstack.test.kvm;

import org.junit.Assert;
import org.junit.Test;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.kvm.KVMHostUtils;

public class KVMHostUtilsTest {
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
