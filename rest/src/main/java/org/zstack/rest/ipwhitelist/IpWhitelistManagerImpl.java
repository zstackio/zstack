package org.zstack.rest.ipwhitelist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigBeforeUpdateExtensionPoint;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.header.Component;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.AccountConstant;
import org.zstack.rest.RestConstants;
import org.zstack.rest.RestGlobalConfig;
import org.zstack.rest.RestServer;
import org.zstack.rest.RestServletRequestInterceptor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

/**
 * Created by lining on 2019/1/15.
 */
public class IpWhitelistManagerImpl implements Component{
    private static final CLogger logger = Utils.getLogger(IpWhitelistManagerImpl.class);

    @Autowired
    RestServer restServer;

    @Override
    public boolean start() {
        RestGlobalConfig.USER_CONNECTION_IP_WHITELIST.installLocalBeforeUpdateExtension(new GlobalConfigBeforeUpdateExtensionPoint() {
            @Override
            public void beforeUpdateExtensionPoint(GlobalConfig oldConfig, String newValue) {
                Gson gson = new GsonBuilder().create();
                IpWhiteListConfigList list = gson.fromJson(newValue, IpWhiteListConfigList.class);
                if (list == null) {
                    throw new GlobalConfigException(String.format("The format is wrong, reference example: %s", AccountConstant.DEFAULT_USER_CONNECTION_IP_WHITELIST));
                }

                for (IpWhitelistConfig config : list) {
                    String errorMsg = IpWhitelistConfigUtils.validateConfigFormat(config);
                    if (errorMsg != null) {
                        throw new GlobalConfigException(errorMsg);
                    }
                }
            }
        });

        restServer.registerRestServletRequestInterceptor(new RestServletRequestInterceptor() {
            @Override
            public void intercept(HttpServletRequest req) throws RestServletRequestInterceptorException {
                boolean enableIpCheck = RestGlobalConfig.ENABLE_USER_CONNECTION_IP_WHITELIST.value(Boolean.class);
                if (!enableIpCheck) {
                    return;
                }

                String userIp = req.getHeader(RestConstants.HEADER_USER_IP);
                if (userIp == null) {
                    return;
                }

                if (!NetworkUtils.isIpv4Address(userIp)) {
                    String error = String.format("The visitor ip[%s] is not an IPv4 address", userIp);
                    throw new RestServletRequestInterceptor.RestServletRequestInterceptorException(HttpStatus.FORBIDDEN.value(), error);
                }

                boolean bool = IpWhitelistConfigUtils.validate(RestGlobalConfig.USER_CONNECTION_IP_WHITELIST.value(), userIp);
                if (!bool) {
                    String error = String.format("Current ip[%s] is forbidden", userIp);
                    throw new RestServletRequestInterceptor.RestServletRequestInterceptorException(HttpStatus.FORBIDDEN.value(), error);
                }
            }
        });

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
