package org.zstack.utils.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONArray;

import java.util.Collection;

public class JSONObjectUtil {
    private static final Gson gson;
    private static final Gson prettyGson;
    
    static {
        gson = new GsonBuilder().create();
        prettyGson = new GsonBuilder().setPrettyPrinting().create();
    }
    
    public static <T, K extends Collection> K toCollection(String content, Class<K> collections, Class<T> clazz) {
        try {
            if (collections.isInterface()) {
                throw new IllegalArgumentException(String.format("collections must be a concrete class, not interface[%s]", collections.getName()));
            }
            Collection c = collections.newInstance();
            JSONArray jarr = new JSONArray(content);
            for (int i=0; i<jarr.length(); i++) {
                String objstr = jarr.getString(i);
                if (String.class != clazz) {
                    c.add(gson.fromJson(objstr, clazz));
                } else {
                    c.add(objstr);
                }
            }
            return (K) c;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static <T> T toObject(String content, Class<T> clazz){
        return gson.fromJson(content, clazz);
    }
    
    public static String toJsonString(Object obj) {
        return gson.toJson(obj);
    }

    public static <T> T rehashObject(Object obj, Class<T> clazz) {
        String str = toJsonString(obj);
        return toObject(str, clazz);
    }

    public static String dumpPretty(Object obj) {
        return prettyGson.toJson(obj);
    }
}
