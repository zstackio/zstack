package org.zstack.sugonSdnController.controller.api;

import com.google.common.collect.ImmutableList;
import com.google.gson.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.MessageCommandRecorder;
import org.zstack.header.rest.RESTFacade;
import org.zstack.sugonSdnController.controller.SugonSdnControllerConstant;
import org.zstack.sugonSdnController.controller.SugonSdnControllerGlobalProperty;
import org.zstack.sugonSdnController.controller.api.types.Domain;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class TfHttpClient<T> {
    private static final CLogger logger = Utils.getLogger(TfHttpClient.class);
    @Autowired
    private RESTFacade restf;
    private TimeUnit unit;
    private Long timeout;
    private String tf_ip;
    private Map<String, String> headers = new HashMap<String, String>() {
        {
            put("Content-Type", "application/json; charset=UTF-8");
        }
    };

    public TfHttpClient(String ip) {
        this.tf_ip = ip;
        this.unit = TimeUnit.MILLISECONDS;
        this.timeout = 30000l;
    }

    public String getTypename(Class<?> cls) {
        String clsname = cls.getName();
        int loc = clsname.lastIndexOf('.');
        if (loc > 0) {
            clsname = clsname.substring(loc + 1);
        }
        String typename = new String();
        for (int i = 0; i < clsname.length(); i++) {
            char ch = clsname.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i > 0) {
                    typename += "-";
                }
                ch = Character.toLowerCase(ch);
            }
            typename += ch;
        }
        return typename;
    }

    public ApiObjectBase jsonToApiObject(String data, Class<? extends ApiObjectBase> cls) {
        if (data == null) {
            return null;
        }
        final String typename = getTypename(cls);
        final JsonParser parser = new JsonParser();
        final JsonObject js_obj = parser.parse(data).getAsJsonObject();
        if (js_obj == null) {
            logger.warn("Unable to parse response");
            return null;
        }
        JsonElement element = null;
        if (cls.getGenericSuperclass() == VRouterApiObjectBase.class) {
            element = js_obj;
        } else {
            element = js_obj.get(typename);
        }
        if (element == null) {
            logger.warn("Element " + typename + ": not found");
            return null;
        }
        ApiObjectBase resp = ApiSerializer.deserialize(element.toString(), cls);
        return resp;
    }

    public List<? extends ApiObjectBase> jsonToApiObjects(String data, Class<? extends ApiObjectBase> cls, boolean withDetail) {
        if (data == null) {
            return null;
        }
        final String typename = getTypename(cls);
        List<ApiObjectBase> list = new ArrayList<ApiObjectBase>();
        final JsonParser parser = new JsonParser();
        final JsonObject js_obj= parser.parse(data).getAsJsonObject();
        if (js_obj == null) {
            logger.warn("Unable to parse response");
            return null;
        }
        final JsonArray array = js_obj.getAsJsonArray(typename + "s");
        if (array == null) {
            logger.warn("Element " + typename + ": not found");
            return null;
        }
        Gson json = ApiSerializer.getDeserializer();
        for (JsonElement element : array) {
            ApiObjectBase obj;
            if (withDetail) {
                obj = jsonToApiObject(element.toString(), cls);
            } else {
                obj = json.fromJson(element.toString(), cls);
            }

            if (obj == null) {
                logger.warn("Unable to decode list element");
                continue;
            }
            list.add(obj);
        }
        return list;
    }

    private String buildUrl(String ip, String path) {
        UriComponentsBuilder ub = UriComponentsBuilder.newInstance();
        ub.scheme("http");
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            ub.host("localhost");
            ub.port(8989);
        } else {
            ub.host(ip);
            ub.port(SugonSdnControllerGlobalProperty.TF_CONTROLLER_PORT);
        }

        ub.path(path);
        return ub.build().toUriString();
    }

    public String buildFqnJsonString(Class<? extends ApiObjectBase> cls, List<String> name_list) {
        Gson json = new Gson();
        JsonObject js_dict = new JsonObject();
        js_dict.add("type", json.toJsonTree(getTypename(cls)));
        js_dict.add("fq_name", json.toJsonTree(name_list));
        return   js_dict.toString();
    }

    public String getUuid(String data) {
        if (data == null) {
            return null;
        }
        final JsonParser parser = new JsonParser();
        final JsonObject js_obj= parser.parse(data).getAsJsonObject();
        if (js_obj == null) {
            logger.warn("Unable to parse response");
            return null;
        }
        final JsonElement element = js_obj.get("uuid");
        if (element == null) {
            logger.warn("Element \"uuid\": not found");
            return null;
        }
        return element.getAsString();
    }

    private ResponseEntity<String> execute(String url, HttpMethod method, String body) {
        HttpHeaders requestHeaders = new HttpHeaders();
        if (headers != null) {
            requestHeaders.setAll(headers);
        }
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<String>(body, requestHeaders);
        ResponseEntity<String> response;
        try {
            response = restf.syncRawJson(buildUrl(tf_ip, url), req, method, unit, timeout);
        } catch (Exception e){
            logger.warn(String.format("Execute http requests:%s failed, reason:%s", url, e.getMessage()));
            return null;
        }

        return response;
    }

    public ApiObjectBase getDomain() {
        TfCommands.GetDomainCmd getDomainCmd = new TfCommands.GetDomainCmd();
        getDomainCmd.type = getTypename(Domain.class);
        List<String> fqName = new ArrayList<>();
        fqName.add(SugonSdnControllerConstant.TF_DEFAULT_DOMAIN);
        getDomainCmd.fq_name = fqName;
        MessageCommandRecorder.record(getDomainCmd.getClass());
        String httpBody = JSONObjectUtil.toJsonString(getDomainCmd);
        TfCommands.GetDomainRsp rsp = restf.syncJsonPost(buildUrl(tf_ip, TfCommands.TF_GET_DAEMON), httpBody, headers, TfCommands.GetDomainRsp.class, unit, timeout);
        return findById(Domain.class, rsp.uuid);
    }

    public synchronized ApiObjectBase findById(Class<? extends ApiObjectBase> cls, String uuid) {
        final String typename = getTypename(cls);
        String url = String.format("/%s/%s", typename, uuid);
        ResponseEntity<String> response = execute(url, HttpMethod.GET, "");

        if (response == null) {
            return null;
        }

        if (response.getStatusCode() != HttpStatus.OK) {
            return null;
        }
        ApiObjectBase object = jsonToApiObject(response.getBody(), cls);
        if (object == null) {
            logger.warn("Unable to decode find response");
        }

        return object;
    }

    public synchronized List<? extends ApiObjectBase> listWithDetail(Class<? extends ApiObjectBase> cls,
                                                                     String fields,
                                                                     String filters) {
        final String typename = getTypename(cls);
        String url = String.format("/%ss?detail=true", typename);
        if (fields != null) {
            url = url + "&fields=" + fields;
        }
        if (filters != null) {
            url = url + "&filters=" + filters;
        }
        ResponseEntity<String> response = execute(url, HttpMethod.GET, "");
        if (response == null) {
            return null;
        }

        if (response.getStatusCode() != HttpStatus.OK) {
            logger.warn("list failed with :" + response.getBody());
            return null;
        }

        String data = response.getBody();
        if (data == null) {
            return null;
        }
        List<? extends ApiObjectBase> list = jsonToApiObjects(data, cls, true);
        if (list == null) {
            logger.warn("Unable to parse/deserialize response: " + data);
        }
        return list;
    }

    public <T extends ApiPropertyBase> List<? extends ApiObjectBase>
    getObjects(Class<? extends ApiObjectBase> cls, List<ObjectReference<T>> refList) {

        List<ApiObjectBase> list = new ArrayList<ApiObjectBase>();
        for (ObjectReference<T> ref : refList) {
            ApiObjectBase obj = findById(cls, ref.getUuid());
            if (obj == null) {
                logger.warn("Unable to find element with uuid: " + ref.getUuid());
                continue;
            }
            list.add(obj);
        }
        return list;
    }

    public ApiObjectBase findByFQN(Class<? extends ApiObjectBase> cls, String fullName) {
        List<String> fqn = ImmutableList.copyOf(StringUtils.split(fullName, ':'));
        String uuid = findByName(cls, fqn);
        if (uuid == null) {
            return null;
        }
        return findById(cls, uuid);
    }

    public synchronized String findByName(Class<? extends ApiObjectBase> cls, List<String> name_list) {
        String jsonStr = buildFqnJsonString(cls, name_list);
        ResponseEntity<String> response = execute("/fqname-to-id", HttpMethod.POST, jsonStr);

        if (response == null) {
            return null;
        }

        if (response.getStatusCode() != HttpStatus.OK) {
            return null;
        }

        String data = response.getBody();
        if (data == null) {
            return null;
        }
        logger.debug("<< Response Data: " + data);

        String uuid = getUuid(data);
        if (uuid == null) {
            logger.warn("Unable to parse response");
            return null;
        }
        return uuid;
    }

    public synchronized Status create(ApiObjectBase obj) {
        final String typename = getTypename(obj.getClass());
        final String jsdata = ApiSerializer.serializeObject(typename, obj);
        String url;

        ResponseEntity<String> response;
        if (obj instanceof VRouterApiObjectBase) {
            url = String.format("/%s", typename);
        } else {
            obj.updateQualifiedName();
            url = String.format("/%ss", typename);
        }
        response = execute(url, HttpMethod.POST, jsdata);

        if (response == null) {
            return Status.failure("No response from API server.");
        }
        HttpStatus status = response.getStatusCode();
        if (status != HttpStatus.OK
                && status != HttpStatus.CREATED
                && status != HttpStatus.ACCEPTED ) {

            String reason =  response.getBody();
            logger.error("create api request failed: " + reason);
            if (status != HttpStatus.NOT_FOUND) {
                logger.error("Failure message: " + reason);
            }
            return Status.failure(reason);
        }

        ApiObjectBase resp = jsonToApiObject(response.getBody(), obj.getClass());
        if (resp == null) {
            String reason = "Unable to decode Create response";
            logger.error(reason);
            return Status.failure(reason);
        }

        String uuid = obj.getUuid();
        if (uuid == null) {
            obj.setUuid(resp.getUuid());
        } else if (!uuid.equals(resp.getUuid())
                && !(obj instanceof VRouterApiObjectBase)) {
            logger.warn("Response contains unexpected uuid: " + resp.getUuid());
            return Status.success();
        }
        logger.debug("Create " + typename + " uuid: " + obj.getUuid());
        return Status.success();
    }

    public synchronized Status update(ApiObjectBase obj) {
        final String typename = getTypename(obj.getClass());
        final String jsdata = ApiSerializer.serializeObject(typename, obj);
        String url = String.format("/%s/%s", typename, obj.getUuid());

        ResponseEntity<String> response = execute(url, HttpMethod.PUT, jsdata);

        if (response == null) {
            return Status.failure("No response from API server.");
        }

        HttpStatus status = response.getStatusCode();
        if (status != HttpStatus.OK
                && status != HttpStatus.ACCEPTED ) {
            String reason = response.getBody();
            logger.warn("<< Response:" + reason);
            return Status.failure(reason);
        }

        return Status.success();
    }

    public synchronized Status delete(Class<? extends ApiObjectBase> cls, String uuid) {
        if (findById(cls, uuid) == null) {
            // object does not exist so we are ok
            return Status.success();
        }

        final String typename = getTypename(cls);
        String url = String.format("/%s/%s", typename, uuid);
        ResponseEntity<String> response = execute(url, HttpMethod.DELETE, "");

        if (response == null) {
            return Status.failure("No response from API server.");
        }

        HttpStatus status = response.getStatusCode();
        if (status != HttpStatus.OK
                && status != HttpStatus.NO_CONTENT
                && status != HttpStatus.ACCEPTED ) {
            String reason = response.getBody();
            logger.warn("Delete failed: " + reason);
            if (status != HttpStatus.NOT_FOUND) {
                logger.error("Failure message: " + response);
            }
            return Status.failure(reason);
        }
        return Status.success();
    }

    public Status delete(ApiObjectBase obj) throws IOException {
        return delete(obj.getClass(), obj.getUuid());
    }
}
