package org.zstack.utils;

/**
 */
public class IptablesUtils {
    public static void appendRuleToFilterTable(String rule) {
        ShellResult ret = ShellUtils.runAndReturn(String.format("/sbin/iptables-save | grep -- '%s' > /dev/null", rule));
        if (ret.getRetCode() == 0) {
            return;
        }

        ret = ShellUtils.runAndReturn(String.format("/sbin/iptables %s", rule));
        ret.raiseExceptionIfFail();
    }

    public static void insertRuleToFilterTable(String rule) {
        insertRuleToFilterTable("iptables", rule);
    }

    public static void insertRuleToFilterTableV6(String rule) {
        insertRuleToFilterTable("ip6tables", rule);
    }

    private static void insertRuleToFilterTable(String command, String rule) {
        ShellResult ret = ShellUtils.runAndReturn(String.format("/sbin/%s-save | grep -- '%s' > /dev/null", command, rule));
        if (ret.getRetCode() == 0) {
            return;
        }

        ret = ShellUtils.runAndReturn(String.format("/sbin/%s %s", command, rule.replace("-A", "-I")));
        ret.raiseExceptionIfFail();
    }

    public static void deleteRuleFromFilterTable(String rule) {
        ShellResult ret = ShellUtils.runAndReturn(String.format("/sbin/iptables-save | grep -- '%s' > /dev/null", rule));
        if (ret.getRetCode() != 0) {
            return;
        }

        ret = ShellUtils.runAndReturn(String.format("/sbin/iptables %s", rule.replace("-A", "-D")));
        ret.raiseExceptionIfFail();
    }
}
