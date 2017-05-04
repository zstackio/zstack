package org.zstack.sdk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by xing5 on 2016/12/9.
 */
public class ZSClient {
    private static OkHttpClient http = new OkHttpClient();

    static final Gson gson;
    static final Gson prettyGson;

    private static ConcurrentHashMap<String, Api> waittingApis = new ConcurrentHashMap<>();

    static {
        gson = new GsonBuilder().create();
        prettyGson = new GsonBuilder().setPrettyPrinting().create();
    }

    private static ZSConfig config;

    public static ZSConfig getConfig() {
        return config;
    }

    public static void configure(ZSConfig c) {
        config = c;

        if (c.readTimeout != null || c.writeTimeout != null) {
            OkHttpClient.Builder b = new OkHttpClient.Builder();

            if (c.readTimeout != null) {
                b.readTimeout(c.readTimeout, TimeUnit.MILLISECONDS);
            }
            if (c.writeTimeout != null) {
                b.writeTimeout(c.writeTimeout, TimeUnit.MILLISECONDS);
            }

            http = b.build();
        }
    }

    public static void webHookCallback(HttpServletRequest req, HttpServletResponse rsp) {
        try {
            StringBuilder jb = new StringBuilder();
            BufferedReader reader = req.getReader();
            String line;

            while ((line = reader.readLine()) != null) {
                jb.append(line);
            }

            String jobUuid = req.getHeader(Constants.HEADER_JOB_UUID);
            if (jobUuid == null) {
                // TODO: log error
                rsp.sendError(400, String.format("missing header[%s]", Constants.HEADER_JOB_UUID));
                return;
            }

            String jobSuccess = req.getHeader(Constants.HEADER_JOB_SUCCESS);
            if (jobSuccess == null) {
                // TODO: log error
                rsp.sendError(400, String.format("missing header[%s]", Constants.HEADER_JOB_SUCCESS));
                return;
            }

            boolean success = Boolean.valueOf(jobSuccess);

            ApiResult res = new ApiResult();
            if (!success) {
                res = gson.fromJson(jb.toString(), ApiResult.class);
            } else {
                res.setResultString(jb.toString());
            }

            Api api = waittingApis.get(jobUuid);
            if (api == null) {
                //TODO: log error
                rsp.sendError(404, String.format("no job[uuid:%s] found", jobUuid));
                return;
            }

            api.wakeUpFromWebHook(res);
            rsp.setStatus(200);
            rsp.getWriter().write("");
        } catch (Exception e) {
            throw new ApiException(e);
        }
    }


    static String join(Collection lst, String sep) {
        String ret = "";
        boolean first = true;
        for (Object s : lst) {
            if (first) {
                ret = s.toString();
                first = false;
                continue;
            }

            ret = ret + sep + s.toString();
        }

        return ret;
    }

    static class Api {
        AbstractAction action;
        RestInfo info;
        InternalCompletion completion;
        String jobUuid = UUID.randomUUID().toString().replaceAll("-", "");

        private ApiResult resultFromWebHook;

        Api(AbstractAction action) {
            this.action = action;
            info = action.getRestInfo();
            if (action.apiId != null) {
                jobUuid = action.apiId;
            }
        }

        void wakeUpFromWebHook(ApiResult res) {
            if (completion == null) {
                resultFromWebHook = res;
                synchronized (this) {
                    this.notifyAll();
                }
            } else {
                try {
                    completion.complete(res);
                } catch (Throwable t) {
                    res = new ApiResult();
                    res.error = new ErrorCode();
                    res.error.code = Constants.INTERNAL_ERROR;
                    res.error.details = t.getMessage();
                    completion.complete(res);
                }
            }
        }

        private String substituteUrl(String url, Map<String, Object> tokens) {
            Pattern pattern = Pattern.compile("\\{(.+?)\\}");
            Matcher matcher = pattern.matcher(url);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                String varName = matcher.group(1);
                Object replacement = tokens.get(varName);
                if (replacement == null) {
                    throw new ApiException(String.format("cannot find value for URL variable[%s]", varName));
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

        void call(InternalCompletion completion) {
            this.completion = completion;
            doCall();
        }

        ApiResult doCall() {
            action.checkParameters();

            Request.Builder reqBuilder = new Request.Builder()
                    .addHeader(Constants.HEADER_JOB_UUID, jobUuid)
                    .addHeader(Constants.HEADER_JSON_SCHEMA, Boolean.TRUE.toString());

            if (config.webHook != null) {
                reqBuilder.addHeader(Constants.HEADER_WEBHOOK, config.webHook);
            }

            if (action instanceof QueryAction) {
                fillQueryApiRequestBuilder(reqBuilder);
            } else {
                fillNonQueryApiRequestBuilder(reqBuilder);
            }

            Request request = reqBuilder.build();

            try {
                if (config.webHook != null) {
                    waittingApis.put(jobUuid, this);
                }

                try (Response response = http.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        return httpError(response.code(), response.body().string());
                    }

                    if (response.code() == 200 || response.code() == 204) {
                        return writeApiResult(response);
                    } else if (response.code() == 202) {

                        if (config.webHook != null) {
                            return webHookResult();
                        } else {
                            return pollResult(response);
                        }
                    } else {
                        throw new ApiException(String.format("[Internal Error] the server returns an unknown status code[%s]", response.code()));
                    }
                }
            } catch (IOException e) {
                throw new ApiException(e);
            }
        }

        private ApiResult syncWebHookResult() {
            synchronized (this) {
                Long timeout = (Long)action.getParameterValue("timeout", false);
                timeout = timeout == null ? config.defaultPollingTimeout : timeout;

                try {
                    this.wait(timeout);
                } catch (InterruptedException e) {
                    throw new ApiException(e);
                }

                if (resultFromWebHook == null) {
                    resultFromWebHook = new ApiResult();
                    resultFromWebHook.error = errorCode(
                            Constants.POLLING_TIMEOUT_ERROR,
                            "timeout of polling async API result",
                            String.format("polling result of api[%s] timeout after %s ms", action.getClass().getSimpleName(), timeout)
                    );
                }

                waittingApis.remove(jobUuid);

                return resultFromWebHook;
            }
        }

        private ApiResult webHookResult() {
            if (completion == null) {
                return syncWebHookResult();
            } else {
                return null;
            }
        }

        private void fillQueryApiRequestBuilder(Request.Builder reqBuilder) {
            QueryAction qaction = (QueryAction) action;

            HttpUrl.Builder urlBuilder = new HttpUrl.Builder().scheme("http")
                    .host(config.hostname)
                    .port(config.port);

            if (config.contextPath != null) {
                urlBuilder.addPathSegments(config.contextPath);
            }

            urlBuilder.addPathSegment("v1")
                    .addPathSegments(info.path.replaceFirst("/", ""));

            if (!qaction.conditions.isEmpty()) {
                for (String cond : qaction.conditions) {
                    urlBuilder.addQueryParameter("q", cond);
                }
            }
            if (qaction.limit != null) {
                urlBuilder.addQueryParameter("limit", String.format("%s", qaction.limit));
            }
            if (qaction.start != null) {
                urlBuilder.addQueryParameter("start", String.format("%s", qaction.start));
            }
            if (qaction.count != null) {
                urlBuilder.addQueryParameter("count", String.format("%s", qaction.count));
            }
            if (qaction.groupBy != null) {
                urlBuilder.addQueryParameter("groupBy", qaction.groupBy);
            }
            if (qaction.replyWithCount != null) {
                urlBuilder.addQueryParameter("replyWithCount", String.format("%s", qaction.replyWithCount));
            }
            if (qaction.sortBy != null) {
                if (qaction.sortDirection == null) {
                    urlBuilder.addQueryParameter("sort", String.format("%s", qaction.sortBy));
                } else {
                    String d = "asc".equals(qaction.sortDirection) ? "+" : "-";
                    urlBuilder.addQueryParameter("sort", String.format("%s%s", d, qaction.replyWithCount));
                }
            }
            if (qaction.fields != null && !qaction.fields.isEmpty()) {
                urlBuilder.addQueryParameter("fields", join(qaction.fields, ","));
            }

            reqBuilder.addHeader(Constants.HEADER_AUTHORIZATION, String.format("%s %s", Constants.OAUTH, qaction.sessionId));
            reqBuilder.url(urlBuilder.build()).get();
        }

        private void fillNonQueryApiRequestBuilder(Request.Builder reqBuilder) {
            HttpUrl.Builder builder = new HttpUrl.Builder()
                    .scheme("http")
                    .host(config.hostname)
                    .port(config.port);

            if (config.contextPath != null) {
                builder.addPathSegments(config.contextPath);
            }

            // HttpUrl will add an extra / to the path segment
            // so /v1/zones will become //v1//zones
            // we remove the extra / here
            builder.addPathSegment("v1");

            List<String> varNames = getVarNamesFromUrl(info.path);
            if (!varNames.isEmpty()) {
                Map<String, Object> vars = new HashMap<>();
                for (String vname : varNames) {
                    Object value = action.getParameterValue(vname);

                    if (value == null) {
                        throw new ApiException(String.format("missing required field[%s]", vname));
                    }

                    vars.put(vname, value);
                }

                String path = substituteUrl(info.path, vars);
                builder.addPathSegments(path.replaceFirst("/", ""));
            } else {
                builder.addPathSegments(info.path.replaceFirst("/", ""));
            }

            final Map<String, Object> params = new HashMap<>();

            for (String pname : action.getAllParameterNames()) {
                if (varNames.contains(pname) || Constants.SESSION_ID.equals(pname)) {
                    // the field is set in URL variables
                    continue;
                }

                Object value = action.getParameterValue(pname);
                if (value != null) {
                    params.put(pname, value);
                }
            }

            if (info.httpMethod.equals("GET") || info.httpMethod.equals("DELETE")) {
                for (Map.Entry<String, Object> e : params.entrySet()) {
                    String k = e.getKey();
                    Object v = e.getValue();

                    if (v instanceof Collection) {
                        for (Object o : (Collection) v) {
                            builder.addQueryParameter(k, o.toString());
                        }
                    } else if (v instanceof Map) {
                        for (Object o : ((Map) v).entrySet()) {
                            Map.Entry pe = (Map.Entry) o;
                            if (!(pe.getKey() instanceof String)) {
                                throw new ApiException(String.format("%s only supports map parameter whose keys and values are both string. %s.%s.%s is not key string",
                                        info.httpMethod, action.getClass(), k, pe.getKey()));
                            }

                            if (pe.getValue() instanceof Collection) {
                                for (Object i : (Collection)pe.getValue()) {
                                    builder.addQueryParameter(String.format("%s.%s", k, pe.getKey()), i.toString());
                                }
                            } else {
                                builder.addQueryParameter(String.format("%s.%s", k, pe.getKey()), pe.getValue().toString());
                            }

                        }
                    } else {
                        builder.addQueryParameter(k, v.toString());
                    }

                }

                if (info.httpMethod.equals("GET")) {
                    reqBuilder.url(builder.build().url().toString()).get();
                } else if (info.httpMethod.equals("DELETE")) {
                    reqBuilder.url(builder.build().url().toString()).delete();
                } else {
                    throw new RuntimeException("should not be here");
                }
            } else {
                Map m = new HashMap();
                m.put(info.parameterName, params);
                reqBuilder.url(builder.build().url().toString()).method(info.httpMethod, RequestBody.create(Constants.JSON, gson.toJson(m)));
            }

            if (info.needSession) {
                Object sessionId = action.getParameterValue(Constants.SESSION_ID);
                reqBuilder.addHeader(Constants.HEADER_AUTHORIZATION, String.format("%s %s", Constants.OAUTH, sessionId));
            }
        }

        private ApiResult pollResult(Response response) throws IOException {
            if (!info.needPoll) {
                throw new ApiException(String.format("[Internal Error] the api[%s] is not an async API but" +
                        " the server returns 201 status code", action.getClass().getSimpleName()));
            }

            Map body = gson.fromJson(response.body().string(), LinkedHashMap.class);
            String pollingUrl = (String) body.get(Constants.LOCATION);
            if (pollingUrl == null) {
                throw new ApiException(String.format("Internal Error] the api[%s] is an async API but the server" +
                        " doesn't return the polling location url", action.getClass().getSimpleName()));
            }

            if (completion == null) {
                // sync polling
                return syncPollResult(pollingUrl);
            } else {
                // async polling
                asyncPollResult(pollingUrl);
                return null;
            }
        }

        private void asyncPollResult(final String url) {
            final long current = System.currentTimeMillis();
            final Long timeout = (Long)action.getParameterValue("timeout", false);
            final long expiredTime = current + (timeout == null ? config.defaultPollingTimeout : timeout);
            final Long i = (Long) action.getParameterValue("pollingInterval", false);

            final Object sessionId = action.getParameterValue(Constants.SESSION_ID);
            final Timer timer = new Timer();

            timer.schedule(new TimerTask() {
                long count = current;
                long interval = i == null ? config.defaultPollingInterval : i;

                private void done(ApiResult res) {
                    completion.complete(res);
                    timer.cancel();
                }

                @Override
                public void run() {
                    Request req = new Request.Builder()
                            .url(url)
                            .addHeader(Constants.HEADER_AUTHORIZATION, String.format("%s %s", Constants.OAUTH, sessionId))
                            .addHeader(Constants.HEADER_JSON_SCHEMA, Boolean.TRUE.toString())
                            .get()
                            .build();

                    try {
                        try (Response response = http.newCall(req).execute()) {
                            if (response.code() != 200 && response.code() != 503 && response.code() != 202) {
                                done(httpError(response.code(), response.body().string()));
                                return;
                            }

                            // 200 means the task has been completed successfully,
                            // or a 505 indicates a failure,
                            // otherwise a 202 returned means it is still
                            // in processing
                            if (response.code() == 200 || response.code() == 503) {
                                done(writeApiResult(response));
                                return;
                            }

                            count += interval;
                            if (count >= expiredTime) {
                                ApiResult res = new ApiResult();
                                res.error = errorCode(
                                        Constants.POLLING_TIMEOUT_ERROR,
                                        "timeout of polling async API result",
                                        String.format("polling result of api[%s] timeout after %s ms", action.getClass().getSimpleName(), timeout)
                                );

                                done(res);
                            }
                        }
                    } catch (Throwable e) {
                        //TODO: logging

                        ApiResult res = new ApiResult();
                        res.error = errorCode(
                                Constants.INTERNAL_ERROR,
                                "an internal error happened",
                                e.getMessage()
                        );

                        done(res);
                    }
                }
            }, 0, i);
        }

        private ErrorCode errorCode(String id, String s, String d) {
            ErrorCode err = new ErrorCode();
            err.code = id;
            err.description = s;
            err.details = d;
            return err;
        }

        private ApiResult syncPollResult(String url) {
            long current = System.currentTimeMillis();
            Long timeout = (Long)action.getParameterValue("timeout", false);
            long expiredTime = current + (timeout == null ? config.defaultPollingTimeout : timeout);
            Long interval = (Long) action.getParameterValue("pollingInterval", false);
            interval = interval == null ? config.defaultPollingInterval : interval;

            Object sessionId = action.getParameterValue(Constants.SESSION_ID);

            while (current < expiredTime) {
                Request req = new Request.Builder()
                        .url(url)
                        .addHeader(Constants.HEADER_AUTHORIZATION, String.format("%s %s", Constants.OAUTH, sessionId))
                        .addHeader(Constants.HEADER_JSON_SCHEMA, Boolean.TRUE.toString())
                        .get()
                        .build();

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
                    throw new ApiException(e);
                }
            }

            ApiResult res = new ApiResult();
            res.error = errorCode(
                    Constants.POLLING_TIMEOUT_ERROR,
                    "timeout of polling async API result",
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
                throw new ApiException(String.format("unknown status code: %s", response.code()));
            }
            return res;
        }

        private ApiResult httpError(int code, String details) {
            ApiResult res = new ApiResult();
            res.error = errorCode(
                    Constants.HTTP_ERROR,
                    String.format("the http status code[%s] indicates a failure happened", code),
                    details
            );
            return res;
        }

        ApiResult call() {
            return doCall();
        }
    }

    private static void errorIfNotConfigured() {
        if (config == null) {
            throw new RuntimeException("setConfig() must be called before any methods");
        }
    }

    static void call(AbstractAction action, InternalCompletion completion) {
        errorIfNotConfigured();
        new Api(action).call(completion);
    }

    static ApiResult call(AbstractAction action) {
        errorIfNotConfigured();
        return new Api(action).call();
    }
}