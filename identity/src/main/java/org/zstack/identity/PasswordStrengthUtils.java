package org.zstack.identity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

    public static String checkPasswordStrength(String password) {
        int minLength = 6;

        if (password.length() < minLength ) {
            return String.format("Password cannot be less than 6 digits in length", minLength);
        }

        if (Weak_Password_Dictionary.contains(password)) {
            return String.format("Password[%s] is a common weak password", password);
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

        if (iPasswordScore < 2) {
            return "Letters, numbers, and punctuation include at least 2";
        }

        return null;
    }
}
