package org.zstack.sdk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.*;
import okhttp3.internal.http.HttpMethod;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by xing5 on 2016/12/9.
 */
public class ZSClient {
    private static final OkHttpClient http = new OkHttpClient();

    static final Gson gson;
    static final Gson prettyGson;

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
    }

    static class Api {
        AbstractAction action;
        RestInfo info;
        InternalCompletion completion;

        Api(AbstractAction action) {
            this.action = action;
            info = action.getRestInfo();
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
                    .addHeader(Constants.HEADER_JSON_SCHEMA, Boolean.TRUE.toString());

            if (action instanceof QueryAction) {
                fillQueryApiRequestBuilder(reqBuilder);
            } else {
                fillNonQueryApiRequestBuilder(reqBuilder);
            }

            Request request = reqBuilder.build();

            try {
                Response response = http.newCall(request).execute();
                if (!response.isSuccessful()) {
                    return httpError(response.code(), response.body().string());
                }

                if (response.code() == 200 || response.code() == 204) {
                    return writeApiResult(response);
                } else if (response.code() == 202) {
                    return pollResult(response);
                } else {
                    throw new ApiException(String.format("[Internal Error] the server returns an unknown status code[%s]", response.code()));
                }
            } catch (IOException e) {
                throw new ApiException(e);
            }
        }

        private String join(Collection lst, String sep) {
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

        private void fillQueryApiRequestBuilder(Request.Builder reqBuilder) {
            QueryAction qaction = (QueryAction) action;

            HttpUrl.Builder urlBuilder = new HttpUrl.Builder().scheme("http")
                    .host(config.hostname)
                    .port(config.port)
                    .addPathSegment("/v1".replaceFirst("/", ""))
                    .addPathSegment(info.path.replaceFirst("/", ""));

            if (!qaction.conditions.isEmpty()) {
                urlBuilder.addQueryParameter("q", join(qaction.conditions, ","));
            }
            if (qaction.limit != null) {
                urlBuilder.addQueryParameter("limit", String.format("%s", qaction.limit));
            }
            if (qaction.start != null) {
                urlBuilder.addQueryParameter("limit", String.format("%s", qaction.start));
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
                    .port(config.port)
                    // HttpUrl will add an extra / to the path segment
                    // so /v1/zones will become //v1//zones
                    // we remove the extra / here
                    .addPathSegment("/v1".replaceFirst("/", ""));

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
                builder.addPathSegment(path.replaceFirst("/", ""));
            } else {
                builder.addPathSegment(info.path.replaceFirst("/", ""));
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

            if (info.httpMethod.equals("GET")) {
                for (Map.Entry<String, Object> e : params.entrySet()) {
                    String k = e.getKey();
                    Object v = e.getValue();

                    if (v instanceof Collection) {
                        for (Object o : (Collection) v) {
                            builder.addQueryParameter(k, o.toString());
                        }
                    } else if (v instanceof Map) {
                        throw new ApiException(String.format("GET won't support map as a parameter type. %s.%s", action.getClass(), k));
                    } else {
                        builder.addQueryParameter(k, v.toString());
                    }
                }

                reqBuilder.url(builder.build().url().toString()).get();
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
                        Response response = http.newCall(req).execute();
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
                    Response response = http.newCall(req).execute();
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