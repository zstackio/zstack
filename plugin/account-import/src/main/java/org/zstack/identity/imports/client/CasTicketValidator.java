package org.zstack.identity.imports.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.jasig.cas.client.authentication.AttributePrincipalImpl;
import org.jasig.cas.client.ssl.HttpURLConnectionFactory;
import org.jasig.cas.client.util.CommonUtils;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.AssertionImpl;
import org.jasig.cas.client.validation.TicketValidationException;
import org.jasig.cas.client.validation.TicketValidator;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

public class CasTicketValidator implements TicketValidator {
    protected static final CLogger logger = Utils.getLogger(CasTicketValidator.class);

    public CasTicketValidator(String casServerUrlPrefix) {
        this.casServerUrlPrefix = casServerUrlPrefix;
    }

    // TODO hard-code
    String casServerUrlPrefix;

    @Override
    public Assertion validate(String ticket, String service) throws TicketValidationException {
        String validationUrl = this.constructValidationUrl(ticket, service);
        logger.debug("Constructing validation url: " + validationUrl);

        try {
            logger.debug("Retrieving response from server.");
            String serverResponse = this.retrieveResponseFromServer(new URL(validationUrl), ticket);
            logger.debug("Server response: " + serverResponse);
            return this.parseResponseFromServer(serverResponse);
        } catch (MalformedURLException var5) {
            throw new TicketValidationException(var5);
        } catch (JsonSyntaxException e) {
            throw new TicketValidationException("failed to parse response to JSON", e);
        }
    }

    /**
     * org.jasig.cas.client.validation.AbstractUrlBasedTicketValidator#constructValidationUrl(java.lang.String, java.lang.String)
     */
    protected final String constructValidationUrl(String ticket, String serviceUrl) {
        Map<String, String> urlParameters = new HashMap<>();
        logger.debug("Placing URL parameters in map.");
        urlParameters.put("ticket", ticket);
        urlParameters.put("service", serviceUrl);
        urlParameters.put("format", "JSON");
//        if (this.renew) {
//            urlParameters.put("renew", "true");
//        }

//        logger.debug("Calling template URL attribute map.");
//        urlParameters.put("pgtUrl", this.proxyCallbackUrl);
        logger.debug("Loading custom parameters from configuration.");

        String suffix = "p3/serviceValidate";
        StringBuilder buffer = new StringBuilder(urlParameters.size() * 10 + this.casServerUrlPrefix.length() + suffix.length() + 1);
        int i = 0;
        buffer.append(this.casServerUrlPrefix);
        if (!this.casServerUrlPrefix.endsWith("/")) {
            buffer.append("/");
        }

        buffer.append(suffix);
        for (Map.Entry<String, String> entry : urlParameters.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value != null) {
                buffer.append(i++ == 0 ? "?" : "&");
                buffer.append(key);
                buffer.append("=");
                String encodedValue = this.encodeUrl(value);
                buffer.append(encodedValue);
            }
        }

        return buffer.toString();
    }

    protected final String encodeUrl(String url) {
        if (url == null) {
            return null;
        } else {
            try {
                return URLEncoder.encode(url, "UTF-8");
            } catch (UnsupportedEncodingException var3) {
                return url;
            }
        }
    }

    /**
     * org.jasig.cas.client.validation.Cas20ServiceTicketValidator#parseResponseFromServer(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    protected final Assertion parseResponseFromServer(String response) throws TicketValidationException {
        JsonElement root = JsonParser.parseString(response);
        if (root == null || !root.isJsonObject()) {
            throw new TicketValidationException("Unable to parse response from server: " + response);
        }

        /*
         If authentication fail, return:
            {
              "serviceResponse": {
                "authenticationFailure": {
                  "code": "INVALID_TICKET",
                  "description": "the ticket `ST-6826b5af-arh4I1UH5wjQgnOMwX2Ei4cimQ-172-24-0-96` is invalid"
                }
              }
            }
         */

        JsonObject serviceResponse = root.getAsJsonObject().getAsJsonObject("serviceResponse");
        if (serviceResponse == null) {
            throw new TicketValidationException("Unable to parse response from server: " + response);
        }

        if (serviceResponse.has("authenticationFailure")) {
            throw new TicketValidationException(
                    serviceResponse.getAsJsonObject("authenticationFailure").get("description").getAsString());
        }

        /*
         If authentication success, return:
            {
              "serviceResponse": {
                "authenticationSuccess": {
                  "user": "<user-name>",
                  "attributes": {
                    "codes": "agentOpen,superAdmin",
                    "appId": "ebe0676358d948329a4720cd017b12ec",
                    "appKey": "c2eab983d67c41f58d62af1512464990",
                    ...
                  }
                }
              }
            }
         */

        JsonElement authenticationSuccessElement = serviceResponse.get("authenticationSuccess");
        if (authenticationSuccessElement == null || !authenticationSuccessElement.isJsonObject()) {
            throw new TicketValidationException("Unable to parse response from server: " + response);
        }

        final JsonObject authenticationSuccess = authenticationSuccessElement.getAsJsonObject();
        String user = authenticationSuccess.get("user").getAsString();
        JsonElement attributesElement = authenticationSuccess.get("attributes");

        AttributePrincipalImpl attributePrincipal;
        if (attributesElement != null && attributesElement.isJsonObject()) {
            Map<String, Object> attributes = (Map<String, Object>) JSONObjectUtil.rehashObject(attributesElement, Map.class);
            attributePrincipal = new AttributePrincipalImpl(user, attributes);
        } else {
            attributePrincipal = new AttributePrincipalImpl(user);
        }

        return new AssertionImpl(attributePrincipal);
    }

    protected final String retrieveResponseFromServer(URL validationUrl, String ticket) {
        return CommonUtils.getResponseFromServer(validationUrl, dummyFactory, null);
    }

    public static class DummyHttpURLConnectionFactory implements HttpURLConnectionFactory {
        private final DummyTrustManager dummyTrustManager = new DummyTrustManager();

        @Override
        public HttpURLConnection buildHttpURLConnection(URLConnection urlConnection) {
            SSLContext sslcontext = null;
            try {
                sslcontext = SSLContext.getInstance("SSL");
                sslcontext.init(null, new TrustManager[] {dummyTrustManager}, new SecureRandom());
                HostnameVerifier ignoreHostnameVerifier = (s, sslsession) -> true;

                HttpsURLConnection conn = (HttpsURLConnection) urlConnection;
                conn.setDefaultHostnameVerifier(ignoreHostnameVerifier);
                conn.setDefaultSSLSocketFactory(sslcontext.getSocketFactory());
                return conn;

            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private final DummyHttpURLConnectionFactory dummyFactory = new DummyHttpURLConnectionFactory();
}
