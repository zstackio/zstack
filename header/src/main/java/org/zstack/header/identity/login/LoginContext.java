package org.zstack.header.identity.login;

import org.zstack.header.identity.APISessionMessage;
import org.zstack.header.identity.SessionInventory;

import java.sql.Timestamp;
import java.util.*;

/**
 *
 */
public class LoginContext {
    /**
     *
     */
    private String username;
    /**
     *
     */
    private String password;
    /**
     *
     */
    private String loginBackendType;
    /**
     *
     */
    private List<String> systemTags;
    /**
     *
     */
    private SessionInventory operatorSession;
    /**
     *
     */
    private String loginPluginName;


    private Map<String, String> properties = new HashMap<>();
    private List<AdditionalAuthFeature> ignoreFeatures = new ArrayList<>();

    private String captchaUuid;
    private String verifyCode;

    private boolean validateOnly;

    private String userUuid;
    private String userType;
    private Timestamp lastUpdatedTime;

    public String getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public Timestamp getLastUpdatedTime() {
        return lastUpdatedTime;
    }

    public void setLastUpdatedTime(Timestamp lastUpdatedTime) {
        this.lastUpdatedTime = lastUpdatedTime;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLoginBackendType() {
        return loginBackendType;
    }

    public void setLoginBackendType(String loginBackendType) {
        this.loginBackendType = loginBackendType;
    }

    public List<String> getSystemTags() {
        return systemTags;
    }

    public void setSystemTags(List<String> systemTags) {
        this.systemTags = systemTags;
    }

    public SessionInventory getOperatorSession() {
        return operatorSession;
    }

    public void setOperatorSession(SessionInventory operatorSession) {
        this.operatorSession = operatorSession;
    }

    public String getCaptchaUuid() {
        return captchaUuid;
    }

    public void setCaptchaUuid(String captchaUuid) {
        this.captchaUuid = captchaUuid;
    }

    public String getVerifyCode() {
        return verifyCode;
    }

    public void setVerifyCode(String verifyCode) {
        this.verifyCode = verifyCode;
    }

    public boolean isValidateOnly() {
        return validateOnly;
    }

    public void setValidateOnly(boolean validateOnly) {
        this.validateOnly = validateOnly;
    }

    public List<AdditionalAuthFeature> getIgnoreFeatures() {
        return ignoreFeatures;
    }

    public void setIgnoreFeatures(List<AdditionalAuthFeature> ignoreFeatures) {
        this.ignoreFeatures = ignoreFeatures;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public String getLoginPluginName() {
        return loginPluginName;
    }

    public void setLoginPluginName(String loginPluginName) {
        this.loginPluginName = loginPluginName;
    }

    public static LoginContext fromApiLoginMessage(APISessionMessage msg) {
        LoginContext params = new LoginContext();
        params.setUsername(msg.getUsername());
        params.setPassword(msg.getPassword());
        params.setLoginBackendType(msg.getLoginType());
        params.setOperatorSession(msg.getSession());
        params.setSystemTags(msg.getSystemTags());

        if (msg instanceof APICaptchaMessage) {
            params.setCaptchaUuid(((APICaptchaMessage) msg).getCaptchaUuid());
            params.setVerifyCode(((APICaptchaMessage) msg).getVerifyCode());
        }

        if (msg instanceof APILogInMsg && (((APILogInMsg) msg).getProperties() != null)) {
            params.getProperties().putAll(((APILogInMsg) msg).getProperties());
        }

        if (params.getSystemTags() == null) {
            params.setSystemTags(Collections.emptyList());
        }

        return params;
    }

    public static LoginContext fromAPIGetLoginProceduresMsg(APIGetLoginProceduresMsg msg) {
        LoginContext params = new LoginContext();
        params.setUsername(msg.getUsername());
        params.setLoginBackendType(msg.getLoginType());
        params.setSystemTags(msg.getSystemTags());

        if (params.getSystemTags() == null) {
            params.setSystemTags(Collections.emptyList());
        }

        return params;
    }

    public static LoginContext fromLoginMessage(LogInMsg msg) {
        LoginContext params = new LoginContext();
        params.setUsername(msg.getUsername());
        params.setPassword(msg.getPassword());
        params.setLoginBackendType(msg.getLoginType());
        params.setOperatorSession(msg.getSession());
        params.setSystemTags(msg.getSystemTags());
        params.setCaptchaUuid(msg.getCaptchaUuid());
        params.setVerifyCode(msg.getVerifyCode());
        params.setValidateOnly(msg.isValidateOnly());

        if (msg.getIgnoreAdditionalFeatures() != null) {
            params.getIgnoreFeatures().addAll(msg.getIgnoreAdditionalFeatures());
        }

        params.getProperties().putAll(msg.getProperties());

        if (params.getSystemTags() == null) {
            params.setSystemTags(Collections.emptyList());
        }

        return params;
    }
}
