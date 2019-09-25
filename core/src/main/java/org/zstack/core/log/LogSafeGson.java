package org.zstack.core.log;

import com.google.gson.*;
import org.apache.logging.log4j.util.Strings;
import org.zstack.header.log.HasSensitiveInfo;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.GsonTransient;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.gson.GsonUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by MaJin on 2019/9/21.
 */
public class LogSafeGson {
    private static Map<Class, Set<Field>> maskFields = new HashMap<>();
    private static Map<Class, Set<Field>> autoFields = new HashMap<>();

    static {
        for (Class<? extends HasSensitiveInfo> si : BeanUtils.reflections.getSubTypesOf(HasSensitiveInfo.class)) {
            List<Field> noLogFs = FieldUtils.getAnnotatedFields(NoLogging.class, si);
            for (Field f : noLogFs) {
                f.setAccessible(true);
                if (f.getAnnotation(NoLogging.class).type().auto()) {
                    autoFields.computeIfAbsent(si, k -> new HashSet<>()).add(f);
                } else {
                    maskFields.computeIfAbsent(si, k -> new HashSet<>()).add(f);
                }
            }

            List<Field> siFs = FieldUtils.getDeclaringClassFields(HasSensitiveInfo.class, si);
            for (Field f : siFs) {
                if (!f.isAnnotationPresent(NoLogging.class)) {
                    f.setAccessible(true);
                    autoFields.computeIfAbsent(si, k -> new HashSet<>()).add(f);
                }
            }
        }
    }

    private static Gson logSafeGson = new GsonUtil().setInstanceCreator(HasSensitiveInfo.class, new JsonSerializer<HasSensitiveInfo>() {

        @Override
        public JsonElement serialize(HasSensitiveInfo o, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject jObj = logSafeGson.toJsonTree(o).getAsJsonObject();
            maskFields.getOrDefault(o.getClass(), Collections.emptySet()).forEach(f -> {
                if (jObj.has(f.getName())) {
                        jObj.addProperty(f.getName(), "*****");
                }
            });

            autoFields.getOrDefault(o.getClass(), Collections.emptySet()).forEach(f -> {
                Object si = getValue(f, o);
                if (si instanceof HasSensitiveInfo) {
                    jObj.add(f.getName(), logSafeGson.toJsonTree(si, HasSensitiveInfo.class));
                }
            });

            return jObj;
        }

    }).setSerializationExclusionStrategy(new ExclusionStrategy() {
        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            return f.getAnnotation(GsonTransient.class) != null;
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }
    }).create();

    public static JsonElement toJsonElement(Object o) {
        if (o instanceof HasSensitiveInfo) {
            return logSafeGson.toJsonTree(o, HasSensitiveInfo.class);
        } else {
            return logSafeGson.toJsonTree(o);
        }
    }

    public static String toJson(Object o) {
        if (o instanceof HasSensitiveInfo) {
            return logSafeGson.toJson(o, HasSensitiveInfo.class);
        } else {
            return logSafeGson.toJson(o);
        }
    }

    public static boolean needMaskLog(Class clz) {
        return maskFields.keySet().contains(clz);
    }

    public static Set<String> getValuesToMask(HasSensitiveInfo o) {
        Set<String> results = new HashSet<>();
        maskFields.getOrDefault(o.getClass(), Collections.emptySet()).forEach(f -> {
            Object obj = getValue(f, o);
            if (obj != null) {
                results.add(obj.toString());
            }
        });

        autoFields.getOrDefault(o.getClass(), Collections.emptySet()).forEach(f -> {
            Object si = getValue(f, o);
            if (si instanceof HasSensitiveInfo) {
                results.addAll(getValuesToMask((HasSensitiveInfo) si));
            }
        });

        results.remove(Strings.EMPTY);
        return results;
    }


    private static Object getValue(Field f, HasSensitiveInfo obj) {
        try {
            return f.get(obj);
        } catch (IllegalAccessException e) {
            return null;
        }
    }
}
