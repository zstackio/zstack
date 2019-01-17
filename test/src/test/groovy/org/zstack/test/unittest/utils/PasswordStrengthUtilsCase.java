package org.zstack.test.unittest.utils;

import org.junit.Test;
import org.zstack.identity.PasswordStrengthUtils;

/**
 * Created by lining on 2019/1/15.
 */
public class PasswordStrengthUtilsCase {
    @Test
    public void checkPasswordStrength() {
        assert null == PasswordStrengthUtils.checkPasswordStrength("password123");
        assert null == PasswordStrengthUtils.checkPasswordStrength("Password123");
        assert null == PasswordStrengthUtils.checkPasswordStrength("PASSWORD123");
        assert null == PasswordStrengthUtils.checkPasswordStrength("&&&12123");
        assert null == PasswordStrengthUtils.checkPasswordStrength("&&&aaaaa");

        assert null != PasswordStrengthUtils.checkPasswordStrength("12345");
        assert null != PasswordStrengthUtils.checkPasswordStrength("iloveyou");
        assert null != PasswordStrengthUtils.checkPasswordStrength("$$$$$$$$");
        assert null != PasswordStrengthUtils.checkPasswordStrength("aaaaaaaa");
        assert null != PasswordStrengthUtils.checkPasswordStrength("asdfasdfasdf");
        assert null != PasswordStrengthUtils.checkPasswordStrength("1111111");
    }
}
