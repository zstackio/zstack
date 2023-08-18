package org.zstack.expon.sdk;

import com.google.common.base.CaseFormat;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.*;
import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.http.HttpMethod;
import org.zstack.core.Platform;
import org.zstack.header.expon.Constants;
import org.zstack.header.rest.DefaultSSLVerifier;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExponClient {
    private static final CLogger logger = Utils.getLogger(ExponClient.class);
    private static OkHttpClient http = new OkHttpClient();

    static final Gson gson;
    private static final DateTimeFormatter formatter;

    private static final long ACTION_DEFAULT_TIMEOUT = -1;
    private static final long ACTION_DEFAULT_POLLINGINTERVAL = -1;

    static {
        gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
        formatter = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("EEE, dd MMM yyyy HH:mm:ss VV")
                .toFormatter(Locale.ENGLISH);
    }

    private ExponConfig config;

    public ExponConfig getConfig() {
        return config;
    }

    public void configure(ExponConfig c) {
        logger.debug("okhttp location " + OkHttpClient.class.getProtectionDomain().getCodeSource().getLocation());
        try {
            logger.debug("okttp version: %s" + Class.forName("okhttp3.OkHttp").getField("VERSION").get(null));
        } catch (Exception e) {
            logger.debug("cannot get okhttp version", e);
        }
        try {
            logger.debug("okttp version: %s" + Class.forName("okhttp3.internal.Version").getDeclaredMethods()[0].invoke(null));
        } catch (Exception e) {
            logger.debug("cannot get okhttp version", e);
        }


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


    static String join(Collection lst, String sep) {
        StringBuilder ret = new StringBuilder();
        boolean first = true;
        for (Object s : lst) {
            if (first) {
                ret = new StringBuilder(s.toString());
                first = false;
                continue;
            }

            ret.append(sep).append(s.toString());
        }

        return ret.toString();
    }

    public <T extends ExponResponse> T call(ExponRequest req, Class<T> clz) {
        ApiResult ret = call(req);
        if (ret.error != null) {
            throw new ExponApiException(ret.error.message);
        }

        return ret.getResult(clz);
    }

    public <T extends ExponQueryResponse> T query(ExponQueryRequest req, Class<T> clz) {
        ApiResult ret = call(req);
        if (ret.error != null) {
            throw new ExponApiException(ret.error.message);
        }

        return ret.getResult(clz);
    }

    class Api {
        ExponRequest action;
        ExponRestRequest restInfo;

        Api(ExponRequest action) {
            this.action = action;
            this.restInfo = action.getClass().getAnnotation(ExponRestRequest.class);
        }

        private String substituteUrl(String url, Map<String, Object> tokens) {
            Pattern pattern = Pattern.compile("\\{(.+?)\\}");
            Matcher matcher = pattern.matcher(url);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                String varName = matcher.group(1);
                Object replacement = tokens.get(varName);
                if (replacement == null) {
                    throw new ExponApiException(String.format("cannot find value for URL variable[%s]", varName));
                }

                matcher.appendReplacement(buffer, "");
                buffer.append(replacement.toString());
            }

            matcher.appendTail(buffer);
            return buffer.toString();
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


        ApiResult doCall() {

            Request.Builder reqBuilder = new Request.Builder();

            try {
                if (action instanceof ExponQueryRequest) {
                    fillQueryApiRequestBuilder(reqBuilder);
                } else {
                    fillNonQueryApiRequestBuilder(reqBuilder);
                }
            } catch (Exception e) {
                throw new ExponApiException(e);
            }

            Request request = reqBuilder.build();
            logger.debug(String.format("request[%s]: %s", action.getClass().getSimpleName(), request.toString()));

            try {
                try (Response response = http.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        if (response.body() == null) {
                            return httpError(response.code(), null);
                        } else {
                            return httpError(response.code(), response.body().string());
                        }
                    }

                    if (response.code() == 200 || response.code() == 204) {
                        return writeApiResult(response);
                    } else if (response.code() == 202) {
                        return pollResult(response);
                    } else {
                        throw new ExponApiException(String.format("[Internal Error] the server returns an unknown status code[%s]", response.code()));
                    }
                }
            } catch (IOException e) {
                throw new ExponApiException(e);
            }
        }

        private ApiResult pollResult(Response response) {
            throw new ExponApiException("not supported yet");
        }

        private void fillQueryApiRequestBuilder(Request.Builder reqBuilder) throws Exception {
            ExponQueryRequest qaction = (ExponQueryRequest) action;

            HttpUrl.Builder urlBuilder = new HttpUrl.Builder().scheme("https")
                    .host(config.hostname)
                    .port(config.port);

            urlBuilder.addPathSegment("api");
            urlBuilder.addPathSegment(restInfo.version());
            urlBuilder.addPathSegments(restInfo.path().replaceFirst("/", ""));

            if (!qaction.conditions.isEmpty()) {
                for (String cond : qaction.conditions) {
                    String[] kv = cond.split("=");
                    urlBuilder.addQueryParameter(kv[0], kv[1]);
                }
            }
            if (qaction.limit != null) {
                urlBuilder.addQueryParameter("offset", String.format("%s", qaction.limit));
            }
            if (qaction.start != null) {
                urlBuilder.addQueryParameter("index", String.format("%s", qaction.start));
            }
            if (qaction.sortBy != null) {
                urlBuilder.addQueryParameter("sort_by", String.format("%s", qaction.sortBy));
            }
            if (qaction.sortDirection != null) {
                urlBuilder.addQueryParameter("order_by", String.format("%s", qaction.sortBy));
            }

            reqBuilder.addHeader(Constants.HEADER_AUTHORIZATION, String.format("%s %s", Constants.BEARER, qaction.sessionId));

            reqBuilder.url(urlBuilder.build()).get();
        }

        private void fillNonQueryApiRequestBuilder(Request.Builder reqBuilder) throws Exception {
            HttpUrl.Builder builder = new HttpUrl.Builder()
                    .scheme("https")
                    .host(config.hostname)
                    .port(config.port);
            builder.addPathSegment("api");
            builder.addPathSegment(restInfo.version());
            if (restInfo.sync()) {
                builder.addPathSegment("sync");
            }

            List<String> varNames = getVarNamesFromUrl(restInfo.path());
            String path = restInfo.path();
            action.initializeParametersIfNot();
            if (!varNames.isEmpty()) {
                Map<String, Object> vars = new HashMap<>();
                for (String vname : varNames) {
                    Object value = action.getParameterValue(vname);

                    if (value == null) {
                        throw new ExponApiException(String.format("missing required field[%s]", vname));
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
                if (varNames.contains(pname) || Constants.SESSION_ID.equals(pname)) {
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
            } else if (restInfo.method().equals(HttpMethod.DELETE)) {
                reqBuilder.url(builder.build()).delete();
            } else {
                reqBuilder.url(builder.build()).method(restInfo.method().toString(), RequestBody.create(Constants.JSON, gson.toJson(params)));
            }

            if (action instanceof LoginExponRequest) {
                return;
            }
            reqBuilder.addHeader(Constants.HEADER_AUTHORIZATION, String.format("%s %s", Constants.BEARER, action.sessionId));
        }

        private ErrorCode errorCode(String id, String s) {
            ErrorCode err = new ErrorCode();
            err.retCode = id;
            err.message = s;
            return err;
        }

        private ApiResult syncPollResult(String url) {
            long current = System.currentTimeMillis();
            long timeout = this.getTimeout();
            long expiredTime = current + timeout;
            long interval = this.getInterval();

            Object sessionId = action.getParameterValue(Constants.SESSION_ID);

            while (current < expiredTime) {
                Request.Builder builder = new Request.Builder()
                        .url(url)
                        .addHeader(Constants.HEADER_AUTHORIZATION, String.format("%s %s", Constants.BEARER, sessionId))
                        .get();

                Request req = builder.build();

                try {
                    try (Response response = http.newCall(req).execute()) {
                        if (response.code() != 200 && response.code() != 503 && response.code() != 202) {
                            return httpError(response.code(), response.body().string());
                        }

                        // 200 means the task has been completed
                        // otherwise a 202 returned means it is still
                        // in processing
                        if (response.code() == 200 || response.code() == 503) {
                            return writeApiResult(response);
                        }

                        TimeUnit.MILLISECONDS.sleep(interval);
                        current += interval;
                    }
                } catch (InterruptedException e) {
                    //ignore
                } catch (IOException e) {
                    Thread.currentThread().interrupt();
                    throw new ExponApiException(e);
                }
            }

            ApiResult res = new ApiResult();
            res.error = errorCode(
                    Constants.POLLING_TIMEOUT_ERROR,
                    String.format("polling result of api[%s] timeout after %s ms", action.getClass().getSimpleName(), timeout)
            );

            return res;
        }

        private ApiResult writeApiResult(Response response) throws IOException {
            ApiResult res = new ApiResult();

            if (response.code() == 200) {
                res.setResultString(response.body().string());
            } else if (response.code() == 503) {
                res = gson.fromJson(response.body().string(), ApiResult.class);
            } else {
                throw new ExponApiException(String.format("unknown status code: %s", response.code()));
            }
            return res;
        }

        private ApiResult httpError(int code, String details) {
            ApiResult res = new ApiResult();
            if (details != null && details.startsWith("{")) {
                ExponResponse rsp = JSONObjectUtil.toObject(details, ExponResponse.class);
                if (rsp.message != null) {
                    res.error = errorCode(
                            rsp.retCode,
                            StringEscapeUtils.unescapeJava(rsp.message)
                    );
                    return res;
                }
            }

            res.error = errorCode(
                    Constants.HTTP_ERROR,
                    String.format("the http status code[%s] details[%s] indicates a failure happened", code, details)
            );
            return res;
        }

        ApiResult call() {
            return doCall();
        }

        private long getTimeout(){
            return action.timeout == ACTION_DEFAULT_TIMEOUT ? config.defaultPollingTimeout: action.timeout;
        }

        private long getInterval(){
            return config.defaultPollingInterval;
        }
    }

    private void errorIfNotConfigured() {
        if (config == null) {
            throw new RuntimeException("setConfig() must be called before any methods");
        }
    }

    public ApiResult call(ExponRequest action) {
        String taskId = Platform.getUuid();
        errorIfNotConfigured();
        logger.debug(String.format("call request[%s]: %s", taskId, gson.toJson(action)));
        ApiResult ret = new Api(action).call();
        logger.debug(String.format("request[%s] result: %s", taskId, gson.toJson(ret)));
        return ret;
    }
}