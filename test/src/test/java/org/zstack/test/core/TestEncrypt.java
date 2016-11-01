package org.zstack.test.core;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.encrypt.EncryptRSA;
import org.zstack.header.core.encrypt.DECRYPT;
import org.zstack.header.core.encrypt.ENCRYPT;
import org.zstack.test.BeanConstructor;

/**
 * Created by mingjian.deng on 16/11/2.
 */
public class TestEncrypt {
    private String password;
    ComponentLoader loader;
    EncryptRSA rsa;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        rsa = loader.getComponent(EncryptRSA.class);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @ENCRYPT
    public void setString(String password){
        this.password = password;
    }

    @DECRYPT
    public String getString(){
        return password;
    }

    @Test
    public void test(){
        setString("pwd");
        Assert.assertNotSame("if encrypt successful, this couldn't be same.", "pwd", getPassword());
        String decreptPassword = getString();
        Assert.assertNotNull(decreptPassword);
        Assert.assertEquals("pwd", getString());
        Assert.assertTrue("pwd".equals(decreptPassword));

        setPassword("test_update");
        Assert.assertEquals("test_update", getString());
    }
}
