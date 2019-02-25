package org.zstack.test.unittest.utils;

import org.junit.Test;
import org.zstack.identity.PasswordStrengthLevel;
import org.zstack.identity.PasswordStrengthUtils;

/**
 * Created by lining on 2019/1/15.
 */
public class PasswordStrengthUtilsCase {
    @Test
    public void checkWeekPassword() {
        assert null == PasswordStrengthUtils.checkPasswordStrength("passwo", PasswordStrengthLevel.weak);
        assert null == PasswordStrengthUtils.checkPasswordStrength("123467", PasswordStrengthLevel.weak);
        assert null == PasswordStrengthUtils.checkPasswordStrength("&&&&&&", PasswordStrengthLevel.weak);
        assert null == PasswordStrengthUtils.checkPasswordStrength("password123!", PasswordStrengthLevel.weak);
        assert null == PasswordStrengthUtils.checkPasswordStrength("Password123*", PasswordStrengthLevel.weak);
        assert null == PasswordStrengthUtils.checkPasswordStrength("PASSWORD123*", PasswordStrengthLevel.weak);
        assert null == PasswordStrengthUtils.checkPasswordStrength("&&&12123a", PasswordStrengthLevel.weak);
        assert null == PasswordStrengthUtils.checkPasswordStrength("&&&aaaaa1", PasswordStrengthLevel.weak);

        assert null != PasswordStrengthUtils.checkPasswordStrength("12345", PasswordStrengthLevel.weak);
        assert null != PasswordStrengthUtils.checkPasswordStrength("$$$$$", PasswordStrengthLevel.weak);
        assert null != PasswordStrengthUtils.checkPasswordStrength("aaa", PasswordStrengthLevel.weak);
        assert null != PasswordStrengthUtils.checkPasswordStrength("1", PasswordStrengthLevel.weak);
    }

    @Test
    public void checkmediumPassword() {
        assert null == PasswordStrengthUtils.checkPasswordStrength("password123", PasswordStrengthLevel.medium);
        assert null == PasswordStrengthUtils.checkPasswordStrength("Password123", PasswordStrengthLevel.medium);
        assert null == PasswordStrengthUtils.checkPasswordStrength("PASSWORD123", PasswordStrengthLevel.medium);
        assert null == PasswordStrengthUtils.checkPasswordStrength("&&&12123", PasswordStrengthLevel.medium);
        assert null == PasswordStrengthUtils.checkPasswordStrength("&&&aaaaa", PasswordStrengthLevel.medium);
        assert null == PasswordStrengthUtils.checkPasswordStrength("password123!", PasswordStrengthLevel.medium);
        assert null == PasswordStrengthUtils.checkPasswordStrength("Password123*", PasswordStrengthLevel.medium);
        assert null == PasswordStrengthUtils.checkPasswordStrength("PASSWORD123*", PasswordStrengthLevel.medium);
        assert null == PasswordStrengthUtils.checkPasswordStrength("&&&12123a", PasswordStrengthLevel.medium);
        assert null == PasswordStrengthUtils.checkPasswordStrength("&&&aaaaa1", PasswordStrengthLevel.medium);

        assert null != PasswordStrengthUtils.checkPasswordStrength("12345", PasswordStrengthLevel.medium);
        assert null != PasswordStrengthUtils.checkPasswordStrength("$$$$$", PasswordStrengthLevel.medium);
        assert null != PasswordStrengthUtils.checkPasswordStrength("aaa", PasswordStrengthLevel.medium);
        assert null != PasswordStrengthUtils.checkPasswordStrength("1", PasswordStrengthLevel.medium);
        assert null != PasswordStrengthUtils.checkPasswordStrength("12345678", PasswordStrengthLevel.medium);
        assert null != PasswordStrengthUtils.checkPasswordStrength("aaaaaaaaaaa", PasswordStrengthLevel.medium);
        assert null != PasswordStrengthUtils.checkPasswordStrength("$$$$$$$$$$", PasswordStrengthLevel.medium);
    }

    @Test
    public void checkStrongPasswordStrength() {
        assert null == PasswordStrengthUtils.checkPasswordStrength("password123!");
        assert null == PasswordStrengthUtils.checkPasswordStrength("Password123*");
        assert null == PasswordStrengthUtils.checkPasswordStrength("PASSWORD123*");
        assert null == PasswordStrengthUtils.checkPasswordStrength("&&&12123a");
        assert null == PasswordStrengthUtils.checkPasswordStrength("&&&aaaaa1");

        assert null != PasswordStrengthUtils.checkPasswordStrength("12345");
        assert null != PasswordStrengthUtils.checkPasswordStrength("iloveyou");
        assert null != PasswordStrengthUtils.checkPasswordStrength("$$$$$$$$");
        assert null != PasswordStrengthUtils.checkPasswordStrength("aaaaaaaa");
        assert null != PasswordStrengthUtils.checkPasswordStrength("asdfasdfasdf");
        assert null != PasswordStrengthUtils.checkPasswordStrength("1111111");
        assert null != PasswordStrengthUtils.checkPasswordStrength("password123");
        assert null != PasswordStrengthUtils.checkPasswordStrength("Password123");
        assert null != PasswordStrengthUtils.checkPasswordStrength("PASSWORD123");
        assert null != PasswordStrengthUtils.checkPasswordStrength("&&&12123");
        assert null != PasswordStrengthUtils.checkPasswordStrength("&&&aaaaa");
    }
}

