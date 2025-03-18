package org.zstack.abstraction.sso;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.abstraction.PluginDriver;

import java.util.Map;

public interface OAuth2ProviderDriver extends PluginDriver {
    default String type() {
        return "sso";
    }

    default String prepareUrl(String requestUrl, MultiValueMap<String, String> map, HttpHeaders headers) {
        return UriComponentsBuilder.fromHttpUrl(requestUrl).toUriString();
    }

    default HttpEntity<MultiValueMap<String, String>> prepareReq(MultiValueMap<String, String> map, HttpHeaders headers) {
        return new HttpEntity<>(map, headers);
    }

    default MultiValueMap<String, String> generateRequestCode(String code, String thirdPartyRedirectUrl, Map<String, String> client) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add(OAuth2PluginConstants.GRANT_TYPE_NAME, OAuth2PluginConstants.GRANT_TYPE_OCDE_VALUE);
        map.add(OAuth2PluginConstants.AUTH_REDIRECT_URL_NAME, thirdPartyRedirectUrl);
        map.add(OAuth2PluginConstants.CLIENT_ID_NAME, client.get("clientId"));
        map.add(OAuth2PluginConstants.CLIENT_SECRET_NAME, client.get("clientSecret"));
        map.add(OAuth2PluginConstants.OAUTH2_CODE, code);
        map.add(OAuth2PluginConstants.AUTH_SCOPE_NAME, StringUtils.isEmpty(client.get("scope")) ? OAuth2PluginConstants.AUTH_SCOPE_VALUE : client.get("scope").replace("::", " "));
        return map;
    }

    default RequestData buildRequestTokenData(MultiValueMap<String, String> map, String requestUrl) {
        return new RequestData(requestUrl, map, HttpMethod.POST, new HttpHeaders());
    }

    default RequestData buildRequestLogOutData(MultiValueMap<String, String> map, String requestUrl) {
        return new RequestData(requestUrl, map, HttpMethod.POST, new HttpHeaders());
    }

    default boolean skipRefreshToken() {
        return false;
    }

    default RequestData buildRequestRefreshTokenData(MultiValueMap<String, String> map, String requestUrl) {
        return new RequestData(requestUrl, map, HttpMethod.POST, new HttpHeaders());
    }

    default RequestData buildRequestTokenIntrospectData(MultiValueMap<String, String> map, String requestUrl, HttpHeaders headers) {
        return new RequestData(requestUrl, map, HttpMethod.POST, headers);
    }

    default Map<String, Object> parseUserInfo(Map<String, Object> response) {
        return response;
    }

    default RequestData buildRequestUserInfoData(MultiValueMap<String, String> map, String requestUrl, HttpHeaders headers) {
        return new RequestData(requestUrl, map, HttpMethod.POST, headers);
    }
}
