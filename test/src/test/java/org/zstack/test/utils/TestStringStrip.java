package org.zstack.test.utils;

import junit.framework.Assert;
import org.junit.Test;
import org.zstack.utils.StringDSL;

/**
 */
public class TestStringStrip {

    @Test
    public void test() {
        String ret = StringDSL.stripStart("Ansible.cfg.forks", "Ansible.cfg.");
        Assert.assertEquals("forks", ret);
        ret = StringDSL.stripEnd("Ansible.cfg.forks", "forks");
        Assert.assertEquals("Ansible.cfg.", ret);
    }
}
