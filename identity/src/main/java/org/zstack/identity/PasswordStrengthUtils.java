package org.zstack.identity;

import java.util.Arrays;
import java.util.List;

/**
 * Created by lining on 2019/1/14.
 */
public class PasswordStrengthUtils {
    private final static List<String> Weak_Password_Dictionary = Arrays.asList(
            "password", "abc123", "iloveyou", "adobe123", "123123", "sunshine",
            "1314520", "a1b2c3", "123qwe", "aaa111", "qweasd", "admin", "passwd",
            "000000","111111","11111111","112233","123123","123321","123456",
            "12345678","654321","666666","888888","abcdef","abcabc","abc123","a1b2c3",
            "aaa111","123qwe","qwerty","qweasd","admin","password","p@ssword","passwd"
            ,"iloveyou","5201314");

    public static String checkPasswordStrength(String password, PasswordStrengthLevel level) {
        int minLength = 6;
        int nonWeakPasswordLength = 8;

        if (password.length() < minLength ) {
            return String.format("Password cannot be less than %s digits in length", minLength);
        }

        if (Weak_Password_Dictionary.contains(password)) {
            return String.format("Password[%s] is a common weak password", password);
        }

        if (level == PasswordStrengthLevel.weak) {
            return null;
        }

        if (password.length() < 8) {
            return String.format("Password cannot be less than %s digits in length", nonWeakPasswordLength);
        }

        //total score of password
        int iPasswordScore = 0;

        boolean containDigit = false;
        boolean containLetter = false;
        boolean containSpecialCharacter = false;

        if (password.matches("(?=.*[0-9]).*")) {
            containDigit = true;
            iPasswordScore ++;
        }

        if (password.matches("(?=.*[a-zA-Z]).*")) {
            containLetter = true;
            iPasswordScore ++;
        }

        if (password.matches("(?=.*[~!@#$%^&*()_-]).*")) {
            containSpecialCharacter = true;
            iPasswordScore ++;
        }

        if (level == PasswordStrengthLevel.medium) {
            if (iPasswordScore < 2) {
                return "Letters, numbers, and punctuation include at least 2";
            }
            return null;
        }

        if (level == PasswordStrengthLevel.strong) {
            if (iPasswordScore < 3) {
                return "Must contain letters, numbers, punctuation";
            }

            return null;
        }

        return null;
    }

    public static String checkPasswordStrength(String password) {
        return checkPasswordStrength(password, PasswordStrengthLevel.strong);
    }
}
