package org.zstack.rest.ipwhitelist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.zstack.utils.network.NetworkUtils;

/**
 * Created by lining on 2019/1/14.
 */
public class IpWhitelistConfigUtils {
    private static final Gson gson = new GsonBuilder().create();

    public static String validateConfigFormat(IpWhitelistConfig config) {
        if (config.getIp() == null) {
            return "ip not configured";
        }

        if (config.getIpAddressRangeType() == null) {
            return String.format("ipAddressRangeType not configured or error, Valid value: %s, %s",
                    IpAddressRangeType.IpRange.toString(),
                    IpAddressRangeType.SingleIp.toString());
        }

        if (config.getState() == null) {
            return String.format("state not configured or error, Valid value: %s, %s",
                    IpWhitelistConfigState.Enabled.toString(),
                    IpWhitelistConfigState.Disabled.toString());
        }

        if (config.getIpAddressRangeType() == IpAddressRangeType.SingleIp) {
            if (!NetworkUtils.isIpv4Address(config.getIp())) {
                return String.format("The ip[%s] is not an IPv4 address", config.getIp());
            }
        } else {
            if (!config.getIp().contains("-")) {
                return String.format("Ip range[%s] format error, reference example: 192.168.0.1-192.168.0.100", config.getIp());
            }

            if (config.getIp().lastIndexOf("-") == config.getIp().length() - 1) {
                return String.format("Ip range[%s] format error, reference example: 192.168.0.1-192.168.0.100", config.getIp());
            }

            String startIp = config.getIp().split("-")[0];
            String endIp = config.getIp().split("-")[1];
            if (!NetworkUtils.isIpv4Address(startIp)) {
                return String.format("The ip[%s] is not an IPv4 address", startIp);
            }
            if (!NetworkUtils.isIpv4Address(startIp)) {
                return String.format("The ip[%s] is not an IPv4 address", endIp);
            }

            try {
                NetworkUtils.validateIpRange(startIp, endIp);
            } catch (IllegalArgumentException e) {
                return String.format("Ip range[%s] configuration error, starting ip needs to be less than the end ip", endIp);
            }
        }

        return null;
    }

    public static boolean validate(String IpWhitelistConfigsJsonString, String ip) {
        IpWhiteListConfigList list = gson.fromJson(IpWhitelistConfigsJsonString, IpWhiteListConfigList.class);

        if (list == null || list.isEmpty()) {
            return false;
        }

        for (IpWhitelistConfig config : list) {
            if (config.getState() == IpWhitelistConfigState.Disabled) {
                continue;
            }

            if (config.getIpAddressRangeType() == IpAddressRangeType.SingleIp) {
                if (ip.equals(config.getIp())) {
                    return true;
                }
            } else if (config.getIpAddressRangeType() == IpAddressRangeType.IpRange) {
                String startIp = config.getIp().split("-")[0];
                String endIp = config.getIp().split("-")[1];
                if (NetworkUtils.isIpv4InRange(ip, startIp, endIp)) {
                    return true;
                }
            } else {
                assert false;
            }
        }

        return false;
    }
}
