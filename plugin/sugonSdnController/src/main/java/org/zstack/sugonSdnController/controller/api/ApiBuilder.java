/*
 * Copyright (c) 2013 Juniper Networks, Inc. All rights reserved.
 */

package org.zstack.sugonSdnController.controller.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

class ApiBuilder {
    private static final CLogger s_logger = Utils.getLogger(ApiBuilder.class);
    ApiBuilder() {
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
            s_logger.warn("Unable to parse response");
            return null;
        }
        JsonElement element = null;
        if (cls.getGenericSuperclass() == VRouterApiObjectBase.class) {
            element = js_obj;
        } else {
            element = js_obj.get(typename);
        }
        if (element == null) {
            s_logger.warn("Element " + typename + ": not found");
            return null;
        }
        ApiObjectBase resp = ApiSerializer.deserialize(element.toString(), cls);
        return resp;
    }

    // body: {"type": class, "fq_name": [parent..., name]}
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
            s_logger.warn("Unable to parse response");
            return null;
        }
        final JsonElement element = js_obj.get("uuid");
        if (element == null) {
            s_logger.warn("Element \"uuid\": not found");
            return null;
        }
        return element.getAsString();
    }

    public List<? extends ApiObjectBase> jsonToApiObjects(String data, Class<? extends ApiObjectBase> cls, boolean withDetail) throws IOException {
        if (data == null) {
            return null;
        }
        final String typename = getTypename(cls);
        List<ApiObjectBase> list = new ArrayList<ApiObjectBase>();
        final JsonParser parser = new JsonParser();
        final JsonObject js_obj= parser.parse(data).getAsJsonObject();
        if (js_obj == null) {
            s_logger.warn("Unable to parse response");
            return null;
        }
        final JsonArray array = js_obj.getAsJsonArray(typename + "s");
        if (array == null) {
            s_logger.warn("Element " + typename + ": not found");
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
                s_logger.warn("Unable to decode list element");
                continue;
            }
            list.add(obj);
        }
        return list;
    }
}
