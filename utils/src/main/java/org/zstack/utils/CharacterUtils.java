package org.zstack.utils;

import org.apache.commons.lang.CharUtils;

/**
 * @Author: fubang
 * @Date: 2018/6/25
 */
public class CharacterUtils {
    public static boolean checkCharacter(String s){
        return s.codePoints().allMatch(code -> CharUtils.isAsciiPrintable((char) code));
    }
}
