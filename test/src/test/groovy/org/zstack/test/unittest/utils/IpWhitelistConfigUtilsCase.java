package org.zstack.test.unittest.utils;

import org.junit.Test;
import org.zstack.rest.ipwhitelist.*;
import org.zstack.utils.gson.JSONObjectUtil;

/**
 * Created by lining on 2019/1/15.
 */
public class IpWhitelistConfigUtilsCase {
    @Test
    public void validateConfigFormat() {
        IpWhitelistConfig config = new IpWhitelistConfig();
        config.setIp("122.11.1.11");
        config.setIpAddressRangeType(IpAddressRangeType.SingleIp);
        config.setState(IpWhitelistConfigState.Enabled);
        assert null == IpWhitelistConfigUtils.validateConfigFormat(config);

        config = new IpWhitelistConfig();
        config.setIp("122.11.1.11-172.11.1.11");
        config.setIpAddressRangeType(IpAddressRangeType.IpRange);
        config.setState(IpWhitelistConfigState.Enabled);
        assert null == IpWhitelistConfigUtils.validateConfigFormat(config);

        config = new IpWhitelistConfig();
        config.setIp("172.216.112.111");
        assert null != IpWhitelistConfigUtils.validateConfigFormat(config);

        config = new IpWhitelistConfig();
        config.setIp("172.211.121.111");
        config.setIpAddressRangeType(IpAddressRangeType.IpRange);
        assert null != IpWhitelistConfigUtils.validateConfigFormat(config);

        config = new IpWhitelistConfig();
        config.setIp("172.211.121.1111");
        config.setIpAddressRangeType(IpAddressRangeType.SingleIp);
        config.setState(IpWhitelistConfigState.Enabled);
        assert null != IpWhitelistConfigUtils.validateConfigFormat(config);

        config = new IpWhitelistConfig();
        config.setIp("289.211.121.1");
        config.setIpAddressRangeType(IpAddressRangeType.SingleIp);
        config.setState(IpWhitelistConfigState.Enabled);
        assert null != IpWhitelistConfigUtils.validateConfigFormat(config);

        config = new IpWhitelistConfig();
        config.setIp("219.211.121.1");
        config.setIpAddressRangeType(IpAddressRangeType.IpRange);
        config.setState(IpWhitelistConfigState.Enabled);
        assert null != IpWhitelistConfigUtils.validateConfigFormat(config);

        config = new IpWhitelistConfig();
        config.setIp("122.11.1.11-172.11.1.11");
        config.setIpAddressRangeType(IpAddressRangeType.SingleIp);
        config.setState(IpWhitelistConfigState.Enabled);
        assert null != IpWhitelistConfigUtils.validateConfigFormat(config);

        config = new IpWhitelistConfig();
        config.setIp("172.11.1.11-122.11.1.11");
        config.setIpAddressRangeType(IpAddressRangeType.IpRange);
        config.setState(IpWhitelistConfigState.Enabled);
        assert null != IpWhitelistConfigUtils.validateConfigFormat(config);
    }

    @Test
    public void validateIpInIpWhitelist() {
        IpWhiteListConfigList configs = new IpWhiteListConfigList();

        IpWhitelistConfig config = new IpWhitelistConfig();
        config.setIp("122.11.1.11");
        config.setIpAddressRangeType(IpAddressRangeType.SingleIp);
        config.setState(IpWhitelistConfigState.Enabled);
        configs.add(config);
        assert IpWhitelistConfigUtils.validate(toString(configs), "122.11.1.11");
        assert !IpWhitelistConfigUtils.validate(toString(configs), "122.11.1.12");
        assert !IpWhitelistConfigUtils.validate(toString(configs), "122.11.1.1");
        configs.clear();

        config.setIp("122.11.1.11");
        config.setIpAddressRangeType(IpAddressRangeType.SingleIp);
        config.setState(IpWhitelistConfigState.Disabled);
        configs.add(config);
        assert !IpWhitelistConfigUtils.validate(toString(configs), "122.11.1.11");
        assert !IpWhitelistConfigUtils.validate(toString(configs), "122.11.1.12");
        assert !IpWhitelistConfigUtils.validate(toString(configs), "122.11.1.1");
        configs.clear();

        config = new IpWhitelistConfig();
        config.setIp("122.11.1.11-172.11.1.11");
        config.setIpAddressRangeType(IpAddressRangeType.IpRange);
        config.setState(IpWhitelistConfigState.Enabled);
        configs.add(config);
        assert IpWhitelistConfigUtils.validate(toString(configs), "122.11.1.11");
        assert IpWhitelistConfigUtils.validate(toString(configs), "122.12.1.11");
        assert IpWhitelistConfigUtils.validate(toString(configs), "172.11.1.11");
        assert !IpWhitelistConfigUtils.validate(toString(configs), "173.11.1.11");
        assert !IpWhitelistConfigUtils.validate(toString(configs), "122.11.1.10");
        configs.clear();

        config = new IpWhitelistConfig();
        config.setIp("122.11.1.11-172.11.1.11");
        config.setIpAddressRangeType(IpAddressRangeType.IpRange);
        config.setState(IpWhitelistConfigState.Disabled);
        configs.add(config);
        assert !IpWhitelistConfigUtils.validate(toString(configs), "122.11.1.11");
        assert !IpWhitelistConfigUtils.validate(toString(configs), "122.12.1.11");
        assert !IpWhitelistConfigUtils.validate(toString(configs), "172.11.1.11");
        assert !IpWhitelistConfigUtils.validate(toString(configs), "173.11.1.11");
        assert !IpWhitelistConfigUtils.validate(toString(configs), "122.11.1.10");
        configs.clear();

        config = new IpWhitelistConfig();
        config.setIp("122.11.1.11");
        config.setIpAddressRangeType(IpAddressRangeType.SingleIp);
        config.setState(IpWhitelistConfigState.Enabled);
        configs.add(config);

        config = new IpWhitelistConfig();
        config.setIp("122.11.1.13-172.11.1.11");
        config.setIpAddressRangeType(IpAddressRangeType.IpRange);
        config.setState(IpWhitelistConfigState.Enabled);
        configs.add(config);
        assert IpWhitelistConfigUtils.validate(toString(configs), "122.11.1.11");
        assert !IpWhitelistConfigUtils.validate(toString(configs), "122.11.1.12");
        assert !IpWhitelistConfigUtils.validate(toString(configs), "122.11.1.1");
        assert IpWhitelistConfigUtils.validate(toString(configs), "122.11.1.11");
        assert IpWhitelistConfigUtils.validate(toString(configs), "122.12.1.11");
        assert IpWhitelistConfigUtils.validate(toString(configs), "172.11.1.11");
        assert !IpWhitelistConfigUtils.validate(toString(configs), "173.11.1.11");
        assert !IpWhitelistConfigUtils.validate(toString(configs), "122.11.1.10");
        configs.clear();

    }

    private String toString(IpWhiteListConfigList configs) {
        return JSONObjectUtil.toJsonString(configs);
    }
}
