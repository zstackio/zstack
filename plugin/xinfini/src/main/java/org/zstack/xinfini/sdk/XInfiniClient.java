package org.zstack.xinfini.sdk;

import com.google.common.base.CaseFormat;
import okhttp3.*;
import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.http.HttpMethod;
import org.zstack.core.Platform;
import org.zstack.externalStorage.sdk.ExternalStorageApiClient;
import org.zstack.header.rest.DefaultSSLVerifier;
import org.zstack.header.xinfini.XInfiniConstants;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.xinfini.NodeStatus;
import org.zstack.xinfini.XInfiniConfig;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XInfiniClient extends ExternalStorageApiClient {
    private static final CLogger logger = Utils.getLogger(XInfiniClient.class);

    private final List<Integer> validHttpStatus = Arrays.asList(200, 201, 202, 404);
    private XInfiniConnectConfig config;

    public XInfiniConnectConfig getConfig() {
        return config;
    }

    public void configure(XInfiniConnectConfig c) {
        config = c;

        if (c.readTimeout != null || c.writeTimeout != null) {
            OkHttpClient.Builder b = new OkHttpClient.Builder();

            if (c.readTimeout != null) {
                b.readTimeout(c.readTimeout, TimeUnit.MILLISECONDS);
            }
            if (c.writeTimeout != null) {
                b.writeTimeout(c.writeTimeout, TimeUnit.MILLISECONDS);
            }

            SSLSocketFactory factory = DefaultSSLVerifier.getSSLFactory(DefaultSSLVerifier.trustAllCerts);
            if (factory != null) {
                http = b.sslSocketFactory(factory, (X509TrustManager) DefaultSSLVerifier.trustAllCerts[0])
                        .hostnameVerifier(DefaultSSLVerifier::verify)
                        .followRedirects(true)
                        .followSslRedirects(true)
                        .build();
            } else {
                http = b.followRedirects(true)
                        .followSslRedirects(true)
                        .build();
            }
        }
    }

    public <T extends XInfiniResponse> T call(XInfiniRequest req, Class<T> clz, XInfiniConfig.Node node) {
        errorIfNotConfigured();
        XinfiniApiResult ret = new Api(req).callWithNode(node);

        if (ret.getMessage() != null) {
            XInfiniResponse rsp = new XInfiniResponse();
            rsp.setReturnCode(ret.getReturnCode());
            rsp.setMessage(ret.getMessage());
            return JSONObjectUtil.rehashObject(rsp, clz);
        }

        return ret.getResult(clz);
    }

    public <T extends XInfiniResponse> T call(XInfiniRequest req, Class<T> clz) {
        return call(req, clz, null);
    }

    public void call(XInfiniRequest req, XInfiniApiCompletion completion) {
        errorIfNotConfigured();
        new Api(req).call(completion);
    }

    class Api {
        XInfiniRequest action;
        XInfiniRestRequest restInfo;

        XInfiniApiCompletion completion;

        final String taskIdForLog = Platform.getUuid();
        String reqBody; // for log

        Api(XInfiniRequest action) {
            this.action = action;
            this.restInfo = action.getClass().getAnnotation(XInfiniRestRequest.class);
        }

        private List<String> getVarNamesFromUrl(String url) {
            Pattern pattern = Pattern.compile("\\{(.+?)\\}");
            Matcher matcher = pattern.matcher(url);

            List<String> urlVars = new ArrayList<>();
            while (matcher.find()) {
                urlVars.add(matcher.group(1));
            }

            return urlVars;
        }

        class WrappedRequest {
            private Map<String, Object> spec;

            public WrappedRequest(Map<String, Object> spec) {
                this.spec = spec;
            }

            public Map<String, Object> getSpec() {
                return spec;
            }

            public void setSpec(Map<String, Object> spec) {
                this.spec = spec;
            }
        }

        XinfiniApiResult doCall() {
            return doCallWithNode(null);
        }

        XinfiniApiResult doCallWithNode(XInfiniConfig.Node withNode) {
            XInfiniConfig.Node node;
            Request.Builder reqBuilder = new Request.Builder();
            action.checkParameters();
            if (withNode != null) {
                node = withNode;
            } else {
                node = config.xInfiniConfig
                        .getNodes()
                        .stream()
                        .filter(it -> it.getStatus() == NodeStatus.Connected)
                        .findAny()
                        .orElseThrow(() -> new XInfiniApiException("No connected node found"));
            }

            try {
                if (action instanceof XInfiniQueryRequest) {
                    fillQueryApiRequestBuilder(reqBuilder, node);
                } else {
                    fillNonQueryApiRequestBuilder(reqBuilder, node);
                }
            } catch (Exception e) {
                throw new XInfiniApiException(e);
            }

            Request request = reqBuilder.build();

            logger.debug(String.format("call request[%s: %s]: %s, body: %s", action.getClass().getSimpleName(), taskIdForLog,
                    request, reqBody));

            try {
                try (Response response = http.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        if (response.body() == null) {
                            return httpError(response.code(), null);
                        } else {
                            return httpError(response.code(), response.body().string());
                        }
                    }

                    if (!validHttpStatus.contains(response.code())) {
                        throw new XInfiniApiException(String.format("[Internal Error] the server returns an unknown status code[%s]", response.code()));
                    }

                    return writeApiResult(response);
                }
            } catch (IOException e) {
                throw new XInfiniApiException(e);
            }
        }

        private void fillQueryApiRequestBuilder(Request.Builder reqBuilder, XInfiniConfig.Node node) throws Exception {
            XInfiniQueryRequest qaction = (XInfiniQueryRequest) action;

            HttpUrl.Builder urlBuilder = new HttpUrl.Builder().scheme("http")
                    .host(node.getIp())
                    .port(node.getPort());

            urlBuilder.addPathSegments(restInfo.category().toString());
            urlBuilder.addPathSegment(restInfo.version());
            urlBuilder.addPathSegments(restInfo.path().replaceFirst("/", ""));
            if (qaction.q != null) {
                urlBuilder.addQueryParameter("q", String.format("%s", qaction.q));
            }
            if (qaction.metric != null) {
                urlBuilder.addQueryParameter("metric", String.format("%s", qaction.metric));
            }
            if (qaction.lables != null) {
                urlBuilder.addQueryParameter("labels", String.format("%s", qaction.lables));
            }
            if (qaction.limit != null) {
                urlBuilder.addQueryParameter("limit", String.format("%s", qaction.limit));
            }
            if (qaction.offset != null) {
                urlBuilder.addQueryParameter("index", String.format("%s", qaction.offset));
            }
            if (qaction.sortBy != null) {
                urlBuilder.addQueryParameter("sort", String.format("%s", qaction.sortBy));
            }

            reqBuilder.addHeader(XInfiniConstants.HEADER_TOKEN, config.token);

            reqBuilder.url(urlBuilder.build()).get();
        }

        private String substituteUrl(String url, Map<String, Object> tokens) {
            Pattern pattern = Pattern.compile("\\{(.+?)\\}");
            Matcher matcher = pattern.matcher(url);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                String varName = matcher.group(1);
                Object replacement = tokens.get(varName);
                if (replacement == null) {
                    throw new XInfiniApiException(String.format("cannot find value for URL variable[%s]", varName));
                }

                matcher.appendReplacement(buffer, "");
                buffer.append(replacement.toString());
            }

            matcher.appendTail(buffer);
            return buffer.toString();
        }

        private void fillNonQueryApiRequestBuilder(Request.Builder reqBuilder, XInfiniConfig.Node node) throws Exception {
            HttpUrl.Builder builder = new HttpUrl.Builder()
                    .scheme("http")
                    .host(node.getIp())
                    .port(node.getPort());
            builder.addPathSegments(restInfo.category().toString());
            builder.addPathSegment(restInfo.version());

            List<String> varNames = getVarNamesFromUrl(restInfo.path());
            String path = restInfo.path();
            if (!varNames.isEmpty()) {
                Map<String, Object> vars = new HashMap<>();
                for (String vname : varNames) {
                    Object value = action.getParameterValue(vname);

                    if (value == null) {
                        throw new XInfiniApiException(String.format("missing required field[%s]", vname));
                    }

                    vars.put(vname, value);
                }

                path = substituteUrl(path, vars);
                builder.addPathSegments(path.replaceFirst("/", ""));
            } else {
                builder.addPathSegments(path.replaceFirst("/", ""));
            }

            final Map<String, Object> params = new HashMap<>();

            for (String pname : action.getAllParameterNames()) {
                if (varNames.contains(pname)) {
                    // the field is set in URL variables
                    continue;
                }

                Object value = action.getParameterValue(pname);
                if (value != null) {
                    params.put(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, pname), value);
                }
            }

            if (restInfo.method().equals(HttpMethod.GET)) {
                reqBuilder.url(builder.build()).get();
            } else if (restInfo.method().equals(HttpMethod.DELETE) && !restInfo.hasBody()) {
                params.forEach((k, v) -> builder.addQueryParameter(k, v.toString()));
                reqBuilder.url(builder.build()).delete();
            } else {
                reqBody = gson.toJson(new WrappedRequest(params));
                reqBuilder.url(builder.build()).method(restInfo.method().toString(), RequestBody.create(XInfiniConstants.JSON, reqBody));
            }

            reqBuilder.addHeader(XInfiniConstants.HEADER_TOKEN, config.token);
        }

        private XinfiniApiResult writeApiResult(Response response) throws IOException {
            XinfiniApiResult res = new XinfiniApiResult();

            if (validHttpStatus.contains(response.code())) {
                res.setReturnCode(response.code());
                String body = response.body().string();
                XInfiniResponse rsp = gson.fromJson(body, XInfiniResponse.class);
                if (rsp.isSuccess()) {
                    res.setResultString(body);
                } else {
                    res.setMessage(StringEscapeUtils.unescapeJava(rsp.getMessage()));
                }
            } else {
                throw new XInfiniApiException(String.format("unknown status code: %s", response.code()));
            }
            return res;
        }

        private XinfiniApiResult httpError(int code, String details) {
            XinfiniApiResult res = new XinfiniApiResult();
            res.setReturnCode(code);
            if (details != null && details.startsWith("{")) {
                XInfiniResponse rsp = gson.fromJson(details, XInfiniResponse.class);
                if (rsp.getMessage() != null) {
                    res.setMessage(StringEscapeUtils.unescapeJava(rsp.getMessage()));
                    return res;
                }
            }

            res.setMessage(String.format("the http status code[%s] details[%s] indicates a failure happened", code, details));
            return res;
        }

        XinfiniApiResult callWithNode(XInfiniConfig.Node node) {
            XinfiniApiResult ret = doCallWithNode(node);
            if (ret.getMessage() != null) {
                logger.debug(String.format("request[%s: %s] error: %s result: %s code: %s", action.getClass().getSimpleName(),
                        taskIdForLog, gson.toJson(ret.getMessage()), ret.getResultString(), ret.getReturnCode()));
            } else {
                logger.debug(String.format("request[%s: %s] result: %s", action.getClass().getSimpleName(),
                        taskIdForLog, ret.getResultString()));
            }
            return ret;
        }

        void call(XInfiniApiCompletion completion) {
            this.completion = completion;
            XinfiniApiResult res = doCall();
            if (res != null) {
                completion.complete(res);
            }
        }

        private long getTimeout() {
            return action.timeout == ACTION_DEFAULT_TIMEOUT ? config.getDefaultPollingTimeout(): action.timeout;
        }

        private long getInterval(){
            return config.getDefaultPollingInterval();
        }
    }

    private void errorIfNotConfigured() {
        if (config == null) {
            throw new RuntimeException("setConfig() must be called before any methods");
        }
    }
}