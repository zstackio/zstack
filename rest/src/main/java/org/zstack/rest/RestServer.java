package org.zstack.rest;

import okhttp3.*;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusEventListener;
import org.zstack.core.retry.Retry;
import org.zstack.core.retry.RetryCondition;
import org.zstack.header.Component;
import org.zstack.header.MapField;
import org.zstack.header.apimediator.ApiMediatorConstant;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.identity.SuppressCredentialCheck;
import org.zstack.header.message.*;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.query.QueryOp;
import org.zstack.header.rest.*;
import org.zstack.rest.sdk.DocumentGenerator;
import org.zstack.rest.sdk.SdkFile;
import org.zstack.rest.sdk.SdkTemplate;
import org.zstack.utils.*;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2016/12/7.
 */
public class RestServer implements Component, CloudBusEventListener {
    private static final CLogger logger = Utils.getLogger(RestServer.class);
    private static final Logger requestLogger = LogManager.getLogger("api.request");
    private static ThreadLocal<RequestInfo> requestInfo = new ThreadLocal<>();

    private static final OkHttpClient http = new OkHttpClient();
    private MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Autowired
    private CloudBus bus;
    @Autowired
    private AsyncRestApiStore asyncStore;
    @Autowired
    private RESTFacade restf;

    static class RequestInfo {
        // don't save session to database as JSON
        // it's not JSON-dumpable
        transient HttpSession session;
        String remoteHost;
        String requestUrl;
        HttpHeaders headers = new HttpHeaders();

        public RequestInfo(HttpServletRequest req) {
            session = req.getSession();
            remoteHost = req.getRemoteHost();

            for (Enumeration e = req.getHeaderNames(); e.hasMoreElements() ;) {
                String name = e.nextElement().toString();
                headers.add(name, req.getHeader(name));
            }

            try {
                requestUrl = URLDecoder.decode(req.getRequestURI(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new CloudRuntimeException(e);
            }
        }
    }

    private static final String ASYNC_JOB_PATH_PATTERN = String.format("%s/%s/{uuid}", RestConstants.API_VERSION, RestConstants.ASYNC_JOB_PATH);

    public static void generateDocTemplate(String path, DocumentGenerator.DocMode mode) {
        DocumentGenerator rg =  GroovyUtils.newInstance("scripts/RestDocumentationGenerator.groovy");
        rg.generateDocTemplates(path, mode);
    }

    public static void generateMarkdownDoc(String path) {
        DocumentGenerator rg =  GroovyUtils.newInstance("scripts/RestDocumentationGenerator.groovy");
        rg.generateMarkDown(path, PathUtil.join(System.getProperty("user.home"), "zstack-markdown"));
    }

    public static void generateJavaSdk() {
        String path = PathUtil.join(System.getProperty("user.home"), "zstack-sdk/java");
        File folder = new File(path);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        try {
            Class clz = GroovyUtils.getClass("scripts/SdkApiTemplate.groovy", RestServer.class.getClassLoader());
            Set<Class<?>> apiClasses = Platform.getReflections().getTypesAnnotatedWith(RestRequest.class)
                    .stream().filter(it -> it.isAnnotationPresent(RestRequest.class)).collect(Collectors.toSet());

            List<SdkFile> allFiles = new ArrayList<>();
            for (Class apiClz : apiClasses) {
                if (Modifier.isAbstract(apiClz.getModifiers())) {
                    continue;
                }

                SdkTemplate tmp = (SdkTemplate) clz.getConstructor(Class.class).newInstance(apiClz);
                allFiles.addAll(tmp.generate());
            }

            SdkTemplate tmp = GroovyUtils.newInstance("scripts/SdkDataStructureGenerator.groovy", RestServer.class.getClassLoader());
            allFiles.addAll(tmp.generate());

            for (SdkFile f : allFiles) {
                //logger.debug(String.format("\n%s", f.getContent()));
                String fpath = PathUtil.join(path, f.getFileName());
                FileUtils.writeStringToFile(new File(fpath), f.getContent());
            }
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public boolean handleEvent(Event e) {
        if (e instanceof APIEvent) {
            RequestData d = asyncStore.complete((APIEvent) e);

            if (d != null && d.webHook != null) {
                try {
                    callWebHook(d);
                } catch (Throwable t) {
                    throw new CloudRuntimeException(t);
                }
            }
        }

        return false;
    }

    static class WebHookRetryException extends RuntimeException {
        public WebHookRetryException() {
        }

        public WebHookRetryException(String message) {
            super(message);
        }

        public WebHookRetryException(String message, Throwable cause) {
            super(message, cause);
        }

        public WebHookRetryException(Throwable cause) {
            super(cause);
        }

        public WebHookRetryException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }

    private void callWebHook(RequestData d) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        requestInfo.set(d.requestInfo);

        AsyncRestQueryResult ret = asyncStore.query(d.apiMessage.getId());

        ApiResponse response = new ApiResponse();
        // task is done
        APIEvent evt = ret.getResult();
        if (evt.isSuccess()) {
            RestResponseWrapper w = responseAnnotationByClass.get(evt.getClass());
            if (w == null) {
                throw new CloudRuntimeException(String.format("cannot find RestResponseWrapper for the class[%s]", evt.getClass()));
            }

            writeResponse(response, w, ret.getResult());
        } else {
            response.setError(evt.getError());
        }

        String body = JSONObjectUtil.toJsonString(response);
        HttpUrl url = HttpUrl.parse(d.webHook);
        Request.Builder rb = new Request.Builder().url(url)
                .post(RequestBody.create(JSON, body))
                .addHeader(RestConstants.HEADER_JOB_UUID, d.apiMessage.getId())
                .addHeader(RestConstants.HEADER_JOB_SUCCESS, String.valueOf(evt.isSuccess()));

        Request request = rb.build();

        new Retry<Void>() {
            String __name__ = String.format("call-webhook-%s", d.webHook);

            @Override
            @RetryCondition(onExceptions = {WebHookRetryException.class}, times = 15, interval = 2)
            protected Void call() {
                try {
                    if (requestLogger.isTraceEnabled()) {
                        StringBuilder sb = new StringBuilder(String.format("Call Web-Hook[%s] (to %s%s)", d.webHook, d.requestInfo.remoteHost, d.requestInfo.requestUrl));
                        sb.append(String.format(" Body: %s", body));

                        requestLogger.trace(sb.toString());
                    }

                    try (Response r = http.newCall(request).execute()) {
                        if (r.code() < 200 || r.code() >= 300) {
                            throw new WebHookRetryException(String.format("failed to post to the webhook[%s], %s",
                                    d.webHook, r.toString()));
                        }
                    }

                } catch (IOException e) {
                    throw new WebHookRetryException(e);
                }

                return null;
            }
        }.run();
    }

    class Api {
        Class apiClass;
        Class apiResponseClass;
        RestRequest requestAnnotation;
        RestResponse responseAnnotation;
        Map<String, String> requestMappingFields;
        String path;
        List<String> optionalPaths = new ArrayList<>();
        String actionName;

        Map<String, Field> allApiClassFields = new HashMap<>();

        @Override
        public String toString() {
            return String.format("%s-%s", requestAnnotation.method(), "null".equals(requestAnnotation.path()) ? apiClass.getName() : path);
        }

        Api(Class clz, RestRequest at) {
            apiClass = clz;
            requestAnnotation = at;
            apiResponseClass = at.responseClass();
            path = String.format("%s%s", RestConstants.API_VERSION, at.path());

            if (at.mappingFields().length > 0) {
                requestMappingFields = new HashMap<>();

                for (String mf : at.mappingFields()) {
                    String[] kv = mf.split("=");
                    if (kv.length != 2) {
                        throw new CloudRuntimeException(String.format("bad requestMappingField[%s] of %s", mf, apiClass));
                    }

                    requestMappingFields.put(kv[0].trim(), kv[1].trim());
                }
            }

            responseAnnotation = (RestResponse) apiResponseClass.getAnnotation(RestResponse.class);
            DebugUtils.Assert(responseAnnotation != null, String.format("%s must be annotated with @RestResponse", apiResponseClass));
            Collections.addAll(optionalPaths, at.optionalPaths());
            optionalPaths = optionalPaths.stream().map( p -> String.format("%s%s", RestConstants.API_VERSION, p)).collect(Collectors.toList());

            if (at.isAction()) {
                actionName = StringUtils.removeStart(apiClass.getSimpleName(), "API");
                actionName = StringUtils.removeEnd(actionName, "Msg");
                actionName = StringUtils.uncapitalize(actionName);
            }

            if (!at.isAction() && requestAnnotation.parameterName().isEmpty() && requestAnnotation.method() == HttpMethod.PUT) {
                throw new CloudRuntimeException(String.format("Invalid @RestRequest of %s, either isAction must be set to true or" +
                        " parameterName is set to a non-empty string", apiClass.getName()));
            }

            List<Field> fs = FieldUtils.getAllFields(apiClass);
            fs = fs.stream().filter(f -> !f.isAnnotationPresent(APINoSee.class) && !Modifier.isStatic(f.getModifiers())).collect(Collectors.toList());
            for (Field f : fs) {
                allApiClassFields.put(f.getName(), f);

                if (requestAnnotation.method() == HttpMethod.GET) {
                    if (APIQueryMessage.class.isAssignableFrom(apiClass)) {
                        // query messages are specially handled
                        continue;
                    }

                    if (Collection.class.isAssignableFrom(f.getType())) {
                        Class gtype = FieldUtils.getGenericType(f);

                        if (gtype == null) {
                            throw new CloudRuntimeException(String.format("%s.%s is of collection type but doesn't not have" +
                                    " a generic type", apiClass, f.getName()));
                        }

                        if (!gtype.getName().startsWith("java.")) {
                            throw new CloudRuntimeException(String.format("%s.%s is of collection type with a generic type" +
                                    "[%s] not belonging to JDK", apiClass, f.getName(), gtype));
                        }
                    } else if (Map.class.isAssignableFrom(f.getType())) {
                        throw new CloudRuntimeException(String.format("%s.%s is of map type, however, the GET method doesn't" +
                                " support query parameters of map type", apiClass, f.getName()));
                    }
                }
            }
        }

        String getMappingField(String key) {
            if (requestMappingFields == null) {
                return null;
            }

            return requestMappingFields.get(key);
        }

        private void mapQueryParameterToApiFieldValue(String name, String[] vals, Map<String, Object> params) throws RestException {
            String[] pairs = name.split("\\.");
            String fname = pairs[0];
            String key = pairs[1];

            Field f = allApiClassFields.get(fname);
            if (f == null) {
                logger.warn(String.format("unknown map query parameter[%s], ignore", name));
                return;
            }

            MapField at = f.getAnnotation(MapField.class);
            DebugUtils.Assert(at!=null, String.format("%s::%s must be annotated by @MapField", apiClass, fname));

            Map m = (Map) params.get(fname);
            if (m == null) {
                m = new HashMap();
                params.put(fname, m);
            }

            if (m.containsKey(key)) {
                throw new RestException(HttpStatus.BAD_REQUEST.value(),
                        String.format("duplicate map query parameter[%s], there has been a parameter with the same map key", name));
            }

            if (Collection.class.isAssignableFrom(at.valueType())) {
                m.put(key, asList(vals));
            } else {
                if (vals.length > 1) {
                    throw new RestException(HttpStatus.BAD_REQUEST.value(),
                            String.format("Invalid query parameter[%s], only one value is allowed for the parameter but" +
                                    " multiple values found", name));
                }

                m.put(key, vals[0]);
            }
        }

        Object queryParameterToApiFieldValue(String name, String[] vals) throws RestException {
            Field f = allApiClassFields.get(name);
            if (f == null) {
                return null;
            }

            if (Collection.class.isAssignableFrom(f.getType())) {
                Class gtype = FieldUtils.getGenericType(f);
                List lst = new ArrayList();
                for (String v : vals) {
                    lst.add(TypeUtils.stringToValue(v, gtype));
                }

                return lst;
            } else {
                if (vals.length > 1) {
                    throw new RestException(HttpStatus.BAD_REQUEST.value(),
                            String.format("Invalid query parameter[%s], only one value is allowed for the parameter but" +
                                    " multiple values found", name));
                }

                return TypeUtils.stringToValue(vals[0], f.getType());
            }
        }
    }

    class RestException extends Exception {
        private int statusCode;
        private String error;

        public RestException(int statusCode, String error) {
            this.statusCode = statusCode;
            this.error = error;
        }
    }

    class RestResponseWrapper {
        RestResponse annotation;
        Map<String, String> responseMappingFields = new HashMap<>();
        Class apiResponseClass;

        public RestResponseWrapper(RestResponse annotation, Class apiResponseClass) {
            this.annotation = annotation;
            this.apiResponseClass = apiResponseClass;

            if (annotation.fieldsTo().length > 0) {
                responseMappingFields = new HashMap<>();

                if (annotation.fieldsTo().length == 1 && "all".equals(annotation.fieldsTo()[0])) {
                    List<Field> apiFields = FieldUtils.getAllFields(apiResponseClass);
                    apiFields = apiFields.stream().filter(f -> !f.isAnnotationPresent(APINoSee.class) && !Modifier.isStatic(f.getModifiers())).collect(Collectors.toList());

                    for (Field f : apiFields) {
                        responseMappingFields.put(f.getName(), f.getName());
                    }
                } else {
                    for (String mf : annotation.fieldsTo()) {
                        String[] kv = mf.split("=");
                        if (kv.length == 2) {
                            responseMappingFields.put(kv[0].trim(), kv[1].trim());
                        } else if (kv.length == 1) {
                            responseMappingFields.put(kv[0].trim(), kv[0].trim());
                        } else {
                            throw new CloudRuntimeException(String.format("bad mappingFields[%s] of %s", mf, apiResponseClass));
                        }

                    }
                }
            }
        }
    }

    void init() throws IllegalAccessException, InstantiationException {
        bus.subscribeEvent(this, new APIEvent());
    }

    private AntPathMatcher matcher = new AntPathMatcher();

    private Map<String, Object> apis = new HashMap<>();
    private Map<Class, RestResponseWrapper> responseAnnotationByClass = new HashMap<>();

    private HttpEntity<String> toHttpEntity(HttpServletRequest req) {
        try {
            String body = IOUtils.toString(req.getReader());
            req.getReader().close();

            HttpHeaders header = new HttpHeaders();
            for (Enumeration e = req.getHeaderNames(); e.hasMoreElements() ;) {
                String name = e.nextElement().toString();
                header.add(name, req.getHeader(name));
            }

            return new HttpEntity<>(body, header);
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
            throw new CloudRuntimeException(e);
        }
    }

    private void sendResponse(int statusCode, String body, HttpServletResponse rsp) throws IOException {
        if (requestLogger.isTraceEnabled()) {
            RequestInfo info = requestInfo.get();

            StringBuilder sb = new StringBuilder(String.format("[ID: %s] Response to %s (%s),", info.session.getId(),
                    info.remoteHost, info.requestUrl));
            sb.append(String.format(" Status Code: %s,", statusCode));
            sb.append(String.format(" Body: %s", body == null || body.isEmpty() ? null : body));

            requestLogger.trace(sb.toString());
        }

        rsp.setStatus(statusCode);
        rsp.getWriter().write(body == null ? "" : body);
    }

    private String getDecodedUrl(HttpServletRequest req) {
        try {
            if (req.getContextPath() == null) {
                return URLDecoder.decode(req.getRequestURI(), "UTF-8");
            } else {
                return URLDecoder.decode(StringUtils.removeStart(req.getRequestURI(), req.getContextPath()), "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException(e);
        }
    }

    void handle(HttpServletRequest req, HttpServletResponse rsp) throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        requestInfo.set(new RequestInfo(req));
        rsp.setCharacterEncoding("utf-8");
        String path = getDecodedUrl(req);
        HttpEntity<String> entity = toHttpEntity(req);

        if (requestLogger.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder(String.format("[ID: %s, Method: %s] Request from %s (to %s), ",
                    req.getSession().getId(), req.getMethod(),
                    req.getRemoteHost(), URLDecoder.decode(req.getRequestURI(), "UTF-8")));
            sb.append(String.format(" Headers: %s,", JSONObjectUtil.toJsonString(entity.getHeaders())));
            if (req.getQueryString() != null && !req.getQueryString().isEmpty()) {
                sb.append(String.format(" Query: %s,", URLDecoder.decode(req.getQueryString(), "UTF-8")));
            }
            sb.append(String.format(" Body: %s", entity.getBody().isEmpty() ? null : entity.getBody()));

            requestLogger.trace(sb.toString());
        }

        if (matcher.match(ASYNC_JOB_PATH_PATTERN, path)) {
            handleJobQuery(req, rsp);
            return;
        }

        Object api = apis.get(path);
        if (api == null) {
            for (String p : apis.keySet()) {
                if (matcher.match(p, path)) {
                    api = apis.get(p);
                    break;
                }
            }
        }

        if (api == null) {
            sendResponse(HttpStatus.NOT_FOUND.value(), String.format("no api mapping to %s", path), rsp);
            return;
        }

        try {
            if (api instanceof Api) {
                handleUniqueApi((Api) api, entity, req, rsp);
            } else {
                handleNonUniqueApi((Collection)api, entity, req, rsp);
            }
        } catch (RestException e) {
            sendResponse(e.statusCode, e.error, rsp);
        } catch (Throwable e) {
            logger.warn(String.format("failed to handle API to %s", path), e);
            sendResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), rsp);
        }
    }

    private void handleJobQuery(HttpServletRequest req, HttpServletResponse rsp) throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        if (!req.getMethod().equals(HttpMethod.GET.name())) {
            sendResponse(HttpStatus.METHOD_NOT_ALLOWED.value(), "only GET method is allowed for querying job status", rsp);
            return;
        }

        Map<String, String> vars = matcher.extractUriTemplateVariables(ASYNC_JOB_PATH_PATTERN, getDecodedUrl(req));
        String uuid = vars.get("uuid");
        AsyncRestQueryResult ret = asyncStore.query(uuid);

        if (ret.getState() == AsyncRestState.expired) {
            sendResponse(HttpStatus.NOT_FOUND.value(), "the job has been expired", rsp);
            return;
        }

        ApiResponse response = new ApiResponse();

        if (ret.getState() == AsyncRestState.processing) {
            sendResponse(HttpStatus.ACCEPTED.value(), response, rsp);
            return;
        }

        // task is done
        APIEvent evt = ret.getResult();
        if (evt.isSuccess()) {
            RestResponseWrapper w = responseAnnotationByClass.get(evt.getClass());
            if (w == null) {
                throw new CloudRuntimeException(String.format("cannot find RestResponseWrapper for the class[%s]", evt.getClass()));
            }
            writeResponse(response, w, ret.getResult());
            sendResponse(HttpStatus.OK.value(), response, rsp);
        } else {
            response.setError(evt.getError());
            sendResponse(HttpStatus.SERVICE_UNAVAILABLE.value(), response, rsp);
        }
    }

    private void sendResponse(int statusCode, ApiResponse response, HttpServletResponse rsp) throws IOException {
        sendResponse(statusCode, response.isEmpty() ? "" : JSONObjectUtil.toJsonString(response), rsp);
    }

    private void handleNonUniqueApi(Collection<Api> apis, HttpEntity<String> entity, HttpServletRequest req, HttpServletResponse rsp) throws RestException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        Map m = JSONObjectUtil.toObject(entity.getBody(), LinkedHashMap.class);
        Api api;

        String parameterName = null;
        if ("POST".equals(req.getMethod())) {
            // create API
            Optional<Api> o = apis.stream().filter(a -> a.requestAnnotation.method().name().equals("POST")).findAny();
            if (!o.isPresent()) {
                throw new RestException(HttpStatus.INTERNAL_SERVER_ERROR.value(), String.format("No creational API found" +
                        " for the path[%s]", req.getRequestURI()));
            }

            api = o.get();
        } else if ("PUT".equals(req.getMethod())) {
            // action API
            Optional<Api> o = apis.stream().filter(a -> m.containsKey(a.actionName)).findAny();

            if (!o.isPresent()) {
                throw new RestException(HttpStatus.BAD_REQUEST.value(), String.format("the body doesn't contain action mapping" +
                        " to the URL[%s]", getDecodedUrl(req)));
            }

            api = o.get();
            parameterName = api.actionName;
        } else if ("GET".equals(req.getMethod())) {
            // query API
            Optional<Api> o = apis.stream().filter(a -> a.requestAnnotation.method().name().equals("GET")).findAny();
            if (!o.isPresent()) {
                throw new RestException(HttpStatus.INTERNAL_SERVER_ERROR.value(), String.format("No query API found" +
                        " for the path[%s]", req.getRequestURI()));
            }

            api = o.get();
        } else if ("DELETE".equals(req.getMethod())) {
            // DELETE API
            Optional<Api> o = apis.stream().filter(a -> a.requestAnnotation.method().name().equals("DELETE")).findAny();
            if (!o.isPresent()) {
                throw new RestException(HttpStatus.INTERNAL_SERVER_ERROR.value(), String.format("No delete API found" +
                        " for the path[%s]", req.getRequestURI()));
            }

            api = o.get();
        } else {
            throw new RestException(HttpStatus.METHOD_NOT_ALLOWED.value(), String.format("The method[%s] is not allowed for" +
                    " the path[%s]", req.getMethod(), req.getRequestURI()));
        }

        parameterName = parameterName == null ? api.requestAnnotation.parameterName() : parameterName;
        handleApi(api, m, parameterName, entity, req, rsp);
    }

    private void handleApi(Api api, Map body, String parameterName, HttpEntity<String> entity, HttpServletRequest req, HttpServletResponse rsp) throws RestException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException, IOException {
        if (body == null) {
            // for some POST request, the body may be null, for example, attach primary storage to a cluster
            body = new HashMap();
        }

        String sessionId = null;
        if (!api.apiClass.isAnnotationPresent(SuppressCredentialCheck.class)) {
            String auth = entity.getHeaders().getFirst("Authorization");
            if (auth == null) {
                throw new RestException(HttpStatus.BAD_REQUEST.value(), "missing header 'Authorization'");
            }

            auth = auth.trim();
            if (!auth.startsWith(RestConstants.HEADER_OAUTH)) {
                throw new RestException(HttpStatus.BAD_REQUEST.value(), String.format("Authorization type must be '%s'", RestConstants.HEADER_OAUTH));
            }

            sessionId = auth.replaceFirst("OAuth", "").trim();
        }

        if (APIQueryMessage.class.isAssignableFrom(api.apiClass)) {
            handleQueryApi(api, sessionId, req, rsp);
            return;
        }

        Object parameter;
        if (req.getMethod().equals(HttpMethod.GET.toString()) || req.getMethod().equals(HttpMethod.DELETE.toString())) {
            // GET uses query string to pass parameters
            Map<String, Object> m = new HashMap<>();

            Map<String, String[]> queryParameters = req.getParameterMap();
            for (Map.Entry<String,  String[]> e : queryParameters.entrySet()) {
                String k = e.getKey();
                String[] vals = e.getValue();

                if (k.contains(".")) {
                    // this is a map parameter
                    api.mapQueryParameterToApiFieldValue(k, vals, m);
                } else {
                    Object val = api.queryParameterToApiFieldValue(k, vals);
                    if (val == null) {
                        logger.warn(String.format("unknown query parameter[%s], ignored", k));
                        continue;
                    }

                    m.put(k, val);
                }
            }

            parameter = m;
        } else {
            parameter = body.get(parameterName);
        }

        APIMessage msg;
        if (parameter == null) {
            msg = (APIMessage) api.apiClass.newInstance();
        } else {
            // check boolean type parameters
            for (Field f : api.apiClass.getDeclaredFields()) {
                if (f.getType().isAssignableFrom(boolean.class)) {
                    Object booleanObject = ((Map) parameter).get(f.getName());
                    if (booleanObject == null) {
                        continue;
                    }
                    String booleanValue = booleanObject.toString();
                    if (!(booleanValue.equalsIgnoreCase("true") ||
                            booleanValue.equalsIgnoreCase("false"))) {
                        throw new RestException(HttpStatus.BAD_REQUEST.value(),
                                String.format("Invalid value for boolean field [%s]," +
                                                " [%s] is not a valid boolean string[true, false].",
                                        f.getName(), booleanValue));
                    }
                }
            }

            msg = JSONObjectUtil.rehashObject(parameter, (Class<APIMessage>) api.apiClass);
        }

        if (requestInfo.get().headers.containsKey(RestConstants.HEADER_JOB_UUID)) {
            String jobUuid = requestInfo.get().headers.get(RestConstants.HEADER_JOB_UUID).get(0);
            if (jobUuid.length() != 32) {
                throw new RestException(HttpStatus.BAD_REQUEST.value(), String.format("Invalid header[%s], it" +
                        " must be a UUID with '-' stripped", RestConstants.HEADER_JOB_UUID));
            }

            msg.setId(jobUuid);
        }

        if (sessionId != null) {
            SessionInventory session = new SessionInventory();
            session.setUuid(sessionId);
            msg.setSession(session);
        }

        if (!req.getMethod().equals(HttpMethod.GET.toString()) && !req.getMethod().equals(HttpMethod.DELETE.toString())) {
            Object systemTags = body.get("systemTags");
            if (systemTags != null) {
                msg.setSystemTags((List<String>) systemTags);
            }

            Object userTags = body.get("userTags");
            if (userTags != null) {
                msg.setUserTags((List<String>) userTags);
            }
        }

        Map<String, String> vars = matcher.extractUriTemplateVariables(api.path, getDecodedUrl(req));
        for (Map.Entry<String, String> e : vars.entrySet()) {
            // set fields parsed from the URL
            String key = e.getKey();
            String mappingKey = api.getMappingField(key);
            PropertyUtils.setProperty(msg, mappingKey == null ? key : mappingKey, e.getValue());
        }

        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        sendMessage(msg, api, rsp);
    }

    private static final LinkedHashMap<String, String> QUERY_OP_MAPPING = new LinkedHashMap();

    static {
        // DO NOT change the order
        // an operator contained by another operator must be placed
        // after the containing operator. For example, "=" is contained
        // by "!=" so it must sit after "!="

        QUERY_OP_MAPPING.put("!=", QueryOp.NOT_EQ.toString());
        QUERY_OP_MAPPING.put(">=", QueryOp.GT_AND_EQ.toString());
        QUERY_OP_MAPPING.put("<=", QueryOp.LT_AND_EQ.toString());
        QUERY_OP_MAPPING.put("!?=", QueryOp.NOT_IN.toString());
        QUERY_OP_MAPPING.put("!~=", QueryOp.NOT_LIKE.toString());
        QUERY_OP_MAPPING.put("~=", QueryOp.LIKE.toString());
        QUERY_OP_MAPPING.put("?=", QueryOp.IN.toString());
        QUERY_OP_MAPPING.put("=", QueryOp.EQ.toString());
        QUERY_OP_MAPPING.put(">", QueryOp.GT.toString());
        QUERY_OP_MAPPING.put("<", QueryOp.LT.toString());
        QUERY_OP_MAPPING.put("is null", QueryOp.IS_NULL.toString());
        QUERY_OP_MAPPING.put("not null", QueryOp.NOT_NULL.toString());
    }

    private void handleQueryApi(Api api, String sessionId, HttpServletRequest req, HttpServletResponse rsp) throws IllegalAccessException, InstantiationException, RestException, IOException, NoSuchMethodException, InvocationTargetException {
        Map<String, String[]> vars = req.getParameterMap();
        APIQueryMessage msg = (APIQueryMessage) api.apiClass.newInstance();

        SessionInventory session = new SessionInventory();
        session.setUuid(sessionId);
        msg.setSession(session);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);

        Map<String, String> urlvars = matcher.extractUriTemplateVariables(api.path, getDecodedUrl(req));
        String uuid = urlvars.get("uuid");
        if (uuid != null) {
            // this is a GET /xxxx/uuid
            // return the resource directly
            QueryCondition qc = new QueryCondition();
            qc.setName("uuid");
            qc.setOp("=");
            qc.setValue(uuid);
            msg.getConditions().add(qc);

            sendMessage(msg, api, rsp);
            return;
        }

        // a query with conditions
        for (Map.Entry<String, String[]> e : vars.entrySet()) {
            String varname = e.getKey().trim();
            String varvalue = e.getValue()[0].trim();

            if ("limit".equals(varname)) {
                try {
                    msg.setLimit(Integer.valueOf(varvalue));
                } catch (NumberFormatException ex) {
                    throw new RestException(HttpStatus.BAD_REQUEST.value(), "Invalid query parameter. 'limit' must be an integer");
                }
            } else if ("start".equals(varname)) {
                try {
                    msg.setStart(Integer.valueOf(varvalue));
                } catch (NumberFormatException ex) {
                    throw new RestException(HttpStatus.BAD_REQUEST.value(), "Invalid query parameter. 'start' must be an integer");
                }
            } else if ("count".equals(varname)) {
                msg.setCount(Boolean.valueOf(varvalue));
            } else if ("groupBy".equals(varname)) {
                msg.setGroupBy(varvalue);
            } else if ("replyWithCount".equals(varname)) {
                msg.setReplyWithCount(Boolean.valueOf(varvalue));
            } else if ("sort".equals(varname)) {
                if (varvalue.startsWith("+")) {
                    msg.setSortDirection("asc");
                    varvalue = StringUtils.stripStart(varvalue, "+");
                } else if (varvalue.startsWith("-")) {
                    msg.setSortDirection("desc");
                    varvalue = StringUtils.stripStart(varvalue, "-");
                } else {
                    msg.setSortDirection("asc");
                }

                msg.setSortBy(varvalue);
            } else if ("q".startsWith(varname)) {
                String[] conds = e.getValue();

                for (String cond : conds) {
                    String OP = null;
                    String delimiter = null;
                    for (String op : QUERY_OP_MAPPING.keySet()) {
                        if (cond.contains(op)) {
                            OP = QUERY_OP_MAPPING.get(op);
                            delimiter = op;
                            break;
                        }
                    }

                    if (OP == null) {
                        throw new RestException(HttpStatus.BAD_REQUEST.value(), String.format("Invalid query parameter." +
                                " The '%s' in the parameter[q] doesn't contain any query operator. Valid query operators are" +
                                " %s", cond, asList(QUERY_OP_MAPPING.keySet())));
                    }

                    QueryCondition qc = new QueryCondition();
                    String[] ks = StringUtils.splitByWholeSeparator(cond, delimiter, 2);
                    if (OP.equals(QueryOp.IS_NULL.toString()) || OP.equals(QueryOp.NOT_NULL.toString())) {
                        String cname = ks[0].trim();
                        qc.setName(cname);
                        qc.setOp(OP);
                    } else {
                        if (ks.length != 2) {
                            throw new RestException(HttpStatus.BAD_REQUEST.value(), String.format("Invalid query parameter." +
                                    " The '%s' in parameter[q] is not a key-value pair split by %s", cond, OP));
                        }

                        String cname = ks[0].trim();
                        String cvalue = ks[1]; // don't trim the value, a space is valid in some conditions
                        qc.setName(cname);
                        qc.setOp(OP);
                        qc.setValue(cvalue);
                    }

                    msg.getConditions().add(qc);
                }
            } else if ("fields".equals(varname)) {
                List<String> fs = new ArrayList<>();
                for (String f : varvalue.split(",")) {
                    fs.add(f.trim());
                }

                if (fs.isEmpty()) {
                    throw new RestException(HttpStatus.BAD_REQUEST.value(), String.format("Invalid query parameter. 'fields'" +
                            " contains zero field"));
                }
                msg.setFields(fs);
            }
        }

        if (msg.getConditions() == null) {
            // no condition specified, query all
            msg.setConditions(new ArrayList<>());
        }

        sendMessage(msg, api, rsp);
    }

    private void handleUniqueApi(Api api, HttpEntity<String> entity, HttpServletRequest req, HttpServletResponse rsp) throws RestException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException, IOException {
        handleApi(api, JSONObjectUtil.toObject(entity.getBody(), LinkedHashMap.class),
               api.requestAnnotation.isAction() ? api.actionName : api.requestAnnotation.parameterName(), entity, req, rsp);
    }

    private void writeResponse(ApiResponse response, RestResponseWrapper w, Object replyOrEvent) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        if (!w.annotation.allTo().equals("")) {
            response.put(w.annotation.allTo(),
                    PropertyUtils.getProperty(replyOrEvent, w.annotation.allTo()));
        } else {
            for (Map.Entry<String, String> e : w.responseMappingFields.entrySet()) {
                response.put(e.getKey(),
                        PropertyUtils.getProperty(replyOrEvent, e.getValue()));
            }
        }

        // TODO: fix hard code hack
        if (APIQueryReply.class.isAssignableFrom(w.apiResponseClass)) {
            Object total = PropertyUtils.getProperty(replyOrEvent, "total");
            if (total != null) {
                response.put("total", total);
            }
        }

        if (requestInfo.get().headers.containsKey(RestConstants.HEADER_JSON_SCHEMA)
                // set schema anyway if it's a query API
                || APIQueryReply.class.isAssignableFrom(w.apiResponseClass)) {
            response.setSchema(new JsonSchemaBuilder(response).build());
        }
    }

    private void sendReplyResponse(MessageReply reply, Api api, HttpServletResponse rsp) throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        ApiResponse response = new ApiResponse();

        if (!reply.isSuccess()) {
            response.setError(reply.getError());
            sendResponse(HttpStatus.SERVICE_UNAVAILABLE.value(), JSONObjectUtil.toJsonString(response), rsp);
            return;
        }

        // the api succeeded

        writeResponse(response, responseAnnotationByClass.get(api.apiResponseClass), reply);
        sendResponse(HttpStatus.OK.value(), response, rsp);
    }

    private void sendMessage(APIMessage msg, Api api, HttpServletResponse rsp) throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        if (msg instanceof APISyncCallMessage) {
            MessageReply reply = bus.call(msg);
            sendReplyResponse(reply, api, rsp);
        } else {
            RequestData d = new RequestData();
            d.apiMessage = msg;
            d.requestInfo = requestInfo.get();
            List<String> webHook = requestInfo.get().headers.get(RestConstants.HEADER_WEBHOOK);
            if (webHook != null && !webHook.isEmpty()) {
                d.webHook = webHook.get(0);
            }

            asyncStore.save(d);
            UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(restf.getBaseUrl());
            ub.path(RestConstants.API_VERSION);
            ub.path(RestConstants.ASYNC_JOB_PATH);
            ub.path("/" + msg.getId());

            ApiResponse response = new ApiResponse();
            response.setLocation(ub.build().toUriString());

            bus.send(msg);

            sendResponse(HttpStatus.ACCEPTED.value(), response, rsp);
        }
    }

    @Override
    public boolean start() {
        build();
        return true;
    }

    private String substituteUrl(String url, Map<String, String> tokens) {
        Pattern pattern = Pattern.compile("\\{(.+?)\\}");
        Matcher matcher = pattern.matcher(url);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            Object replacement = tokens.get(varName);
            if (replacement == null) {
                throw new CloudRuntimeException(String.format("cannot find value for URL variable[%s]", varName));
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

    private String normalizePath(String p) {
        // normalize the path,
        // paths for example /backup-storage/{backupStorageUuid}/actions
        // and /backup-storage/{uuid}/actions are treated as equal,
        // and will be normalized to /backup-storage/{0}/actions

        List<String> varNames = getVarNamesFromUrl(p);
        if (varNames.isEmpty()) {
            return p;
        }

        Map<String, String> m = new HashMap<>();
        for (int i=0; i<varNames.size(); i++) {
            m.put(varNames.get(i), String.format("{%s}", i));
        }

        return substituteUrl(p, m);
    }

    private void collectRestRequestErrConfigApi(List<String> errorApiList, Class apiClass, RestRequest apiRestRequest){
        if (apiRestRequest.isAction() && !RESTConstant.DEFAULT_PARAMETER_NAME.equals(apiRestRequest.parameterName())) {
            errorApiList.add(String.format("[%s] RestRequest config error, Setting parameterName is not allowed when isAction set true", apiClass.getName()));
        } else if (apiRestRequest.isAction() && HttpMethod.PUT != apiRestRequest.method()) {
            errorApiList.add(String.format("[%s] RestRequest config error, method can only be set to HttpMethod.PUT when isAction set true", apiClass.getName()));
        }else if (!RESTConstant.DEFAULT_PARAMETER_NAME.equals(apiRestRequest.parameterName()) && (HttpMethod.PUT == apiRestRequest.method() || HttpMethod.DELETE == apiRestRequest.method())){
            errorApiList.add(String.format("[%s] RestRequest config error, method is not allowed to set to HttpMethod.PUT(HttpMethod.DELETE) when parameterName set a value", apiClass.getName()));
        }else if(HttpMethod.GET == apiRestRequest.method() && !RESTConstant.DEFAULT_PARAMETER_NAME.equals(apiRestRequest.parameterName())){
            errorApiList.add(String.format("[%s] RestRequest config error, Setting parameterName is not allowed when method set HttpMethod.GET", apiClass.getName()));
        }
    }

    private void build() {
        Reflections reflections = Platform.getReflections();
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(RestRequest.class).stream()
                .filter(it -> it.isAnnotationPresent(RestRequest.class)).collect(Collectors.toSet());

        List<String> errorApiList = new ArrayList();
        for (Class clz : classes) {
            RestRequest at = (RestRequest) clz.getAnnotation(RestRequest.class);
            Api api = new Api(clz, at);

            collectRestRequestErrConfigApi(errorApiList, clz, at);

            List<String> paths = new ArrayList<>();
            if (!"null".equals(api.path)) {
                paths.add(api.path);
            }
            paths.addAll(api.optionalPaths);


            for (String path : paths) {
                String normalizedPath = normalizePath(path);

                api = new Api(clz, at);
                api.path = path;

                if (!apis.containsKey(normalizedPath)) {
                    apis.put(normalizedPath, api);
                } else {
                    Object c = apis.get(normalizedPath);

                    List lst;
                    if (c instanceof Api) {
                        // merge to a list
                        lst = new ArrayList();
                        lst.add(c);

                        apis.put(normalizedPath, lst);
                    } else {
                        lst = (List) c;
                    }

                    lst.add(api);
                }
            }

            responseAnnotationByClass.put(api.apiResponseClass, new RestResponseWrapper(api.responseAnnotation, api.apiResponseClass));
        }

        responseAnnotationByClass.put(APIEvent.class, new RestResponseWrapper(new RestResponse(){
            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public String allTo() {
                return "";
            }

            @Override
            public String[] fieldsTo() {
                return new String[0];
            }

        }, APIEvent.class));

        if (errorApiList.size() > 0){
            logger.error(String.format("Error Api list : %s", errorApiList));
            throw new RuntimeException(String.format("Error Api list : %s", errorApiList));
        }

        // below codes are checking if there
        // are duplicated APIs
        for (Object o : apis.values()) {
            if (!(o instanceof List)) {
                continue;
            }

            List<Api> as = (List<Api>) o;
            List<Api> nonActions = as.stream().filter(a -> !a.requestAnnotation.isAction()).collect(Collectors.toList());

            Map<String, Api> set = new HashMap<>();
            for (Api a : nonActions) {
                Api old = set.get(a.toString());
                if (old != null) {
                    throw new CloudRuntimeException(String.format("duplicate rest API[%s, %s], they both have the same" +
                            " HTTP methods and paths, and both are not actions. %s", a.apiClass, old.apiClass, a.toString()));
                }

                set.put(a.toString(), a);
            }

            List<Api> actions = as.stream().filter(a -> a.requestAnnotation.isAction()).collect(Collectors.toList());
            set = new HashMap<>();
            for (Api a : actions) {
                Api old = set.get(a.actionName);
                if (old != null) {
                    throw new CloudRuntimeException(String.format("duplicate rest API[%s, %s], they are both actions with the" +
                            " same action name[%s]", a.apiClass, old.apiClass, a.actionName));
                }

                set.put(a.actionName, a);
            }
        }
    }

    @Override
    public boolean stop() {
        return true;
    }
}
