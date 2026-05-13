package org.zstack.utils.ssh;

import org.junit.Assert;
import org.junit.Test;

public class SshTest {
    @Test
    public void addLegacyHostKeyAlgorithmsForOldVyosSshd() {
        Assert.assertEquals("ssh-ed25519,rsa-sha2-512,ssh-rsa,ssh-dss",
                Ssh.appendSshAlgorithms("ssh-ed25519,rsa-sha2-512", "ssh-rsa", "ssh-dss"));
    }

    @Test
    public void doNotDuplicateLegacyHostKeyAlgorithms() {
        Assert.assertEquals("ssh-ed25519,ssh-rsa,ssh-dss",
                Ssh.appendSshAlgorithms("ssh-ed25519,ssh-rsa", "ssh-rsa", "ssh-dss"));
    }

    @Test
    public void useLegacyHostKeyAlgorithmsWhenConfigIsEmpty() {
        Assert.assertEquals("ssh-rsa,ssh-dss",
                Ssh.appendSshAlgorithms(null, "ssh-rsa", "ssh-dss"));
    }
}
