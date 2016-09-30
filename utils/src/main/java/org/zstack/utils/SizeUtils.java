package org.zstack.utils;

import org.zstack.utils.data.SizeUnit;

import java.util.Arrays;
import java.util.List;

/**
 */
public class SizeUtils {
    private static final String T_SUFFIX = "T";
    private static final String t_SUFFIX = "t";
    private static final String G_SUFFIX = "G";
    private static final String g_SUFFIX = "g";
    private static final String M_SUFFIX = "M";
    private static final String m_SUFFIX = "m";
    private static final String K_SUFFIX = "K";
    private static final String k_SUFFIX = "k";
    private static final String B_SUFFIX = "B";
    private static final String b_SUFFIX = "b";

    private static final List<String> validSuffix = Arrays.asList(
            T_SUFFIX,
            t_SUFFIX,
            G_SUFFIX,
            g_SUFFIX,
            M_SUFFIX,
            m_SUFFIX,
            K_SUFFIX,
            k_SUFFIX,
            B_SUFFIX,
            b_SUFFIX
    );

    private static boolean isPositiveNumber(String str) {
        return str.matches("\\d+");
    }

    public static boolean isSizeString(String str) {
        String suffix = str.substring(str.length() - 1);
        if (!validSuffix.contains(suffix)) {
            return isPositiveNumber(str);
        } else {
            String num = str.substring(0, str.length()-1);
            return isPositiveNumber(num);
        }
    }

    public static long sizeStringToBytes(String str) {
        String numStr = str.substring(0, str.length() - 1);
        String suffix = str.substring(str.length() - 1);
        if (!validSuffix.contains(suffix)) {
            return Long.valueOf(str);
        }

        Long size = Long.valueOf(numStr);
        if (suffix.equals(T_SUFFIX) || suffix.equals(t_SUFFIX)) {
            return SizeUnit.TERABYTE.toByte(size);
        } else if (suffix.equals(G_SUFFIX) || suffix.equals(g_SUFFIX)) {
            return SizeUnit.GIGABYTE.toByte(size);
        } else if (suffix.equals(M_SUFFIX) || suffix.equals(m_SUFFIX)) {
            return SizeUnit.MEGABYTE.toByte(size);
        } else if (suffix.equals(K_SUFFIX) || suffix.equals(k_SUFFIX)) {
            return SizeUnit.KILOBYTE.toByte(size);
        } else if (suffix.equals(B_SUFFIX) || suffix.equals(b_SUFFIX)) {
            return SizeUnit.BYTE.toByte(size);
        }

        throw new RuntimeException("should not be here," + str);
    }
}
