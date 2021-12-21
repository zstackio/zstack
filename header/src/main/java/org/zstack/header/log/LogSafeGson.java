package org.zstack.header.log;

import com.google.gson.*;
import org.apache.logging.log4j.util.Strings;
import org.zstack.header.message.GsonTransient;
import org.zstack.header.message.Message;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.GsonUtil;
import org.zstack.utils.gson.JSONObjectUtil;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by MaJin on 2019/9/21.
 */
public class LogSafeGson {
    private static Map<Class, Set<FieldNoLogging>> maskFields = new HashMap<>();
    private static Map<Class, Set<FieldNoLogging>> autoFields = new HashMap<>();

    private static final List<Class<?>> searchClasses = Arrays.asList(Serializable.class, Message.class);
    private static final Gson logSafeGson;
    private static Function<String, String> tagInfoHider = s -> s;
    private static class FieldNoLogging {
        Field field;
        NoLogging annotation;
        Field classNameField;

        private static Pattern uriPattern = Pattern.compile(":[^:]*@");

        FieldNoLogging(Field field) {
            this.field = field;
        }

        FieldNoLogging(Field field, NoLogging annotation, Class<?> senClz) {
            this.field = field;
            this.annotation = annotation;
            if (!Strings.isEmpty(annotation.classNameField())) {
                this.classNameField = FieldUtils.getField(annotation.classNameField(), senClz);
                if (this.classNameField != null) {
                    this.classNameField.setAccessible(true);
                }
            }
        }

        String getName() {
            return field.getName();
        }

        Object getValue(Object obj) {
            try {
                Object value = field.get(obj);
                if (classNameField == null) {
                    return value;
                }

                Class<?> clz = Class.forName((String) classNameField.get(obj));
                return mayHasSensitiveInfo(clz) ? JSONObjectUtil.toObject((String) value, clz) : value;
            } catch (IllegalAccessException | ClassNotFoundException e) {
                return null;
            }
        }

        String getMaskedValue(String raw) {
            if (annotation.type().simple()) {
                return "*****";
            } else if (annotation.type().tag()) {
                return tagInfoHider.call(raw);
            } else {
                return Utils.getLogMaskWords().getOrDefault(raw, uriPattern.matcher(raw).replaceFirst(":*****@"));
            }
        }
    }

    static {
        GsonUtil util = new GsonUtil();
        searchClasses.forEach(it -> util.setInstanceCreator(it, getSerializer()));
        logSafeGson = util.setSerializationExclusionStrategy(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes f) {
                return f.getAnnotation(GsonTransient.class) != null;
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        }).create();

        for (Class<?> baseClz : searchClasses) {
            for (Class<?> clz : BeanUtils.reflections.getSubTypesOf(baseClz)) {
                if (!clz.isInterface()) {
                    cacheNoLoggingInfo(clz);
                }

            }
        }
    }

    private static void cacheNoLoggingInfo(Class<?> si) {
        for (Field f : FieldUtils.getAllFields(si)) {
            NoLogging an = f.getAnnotation(NoLogging.class);
            if (an != null) {
                f.setAccessible(true);
                if (an.behavior().auto()) {
                    autoFields.computeIfAbsent(si, k -> new HashSet<>()).add(new FieldNoLogging(f, an, si));
                } else {
                    maskFields.computeIfAbsent(si, k -> new HashSet<>()).add(new FieldNoLogging(f, an, si));
                }
            } else if (mayHasSensitiveInfo(f.getType()) && !f.getType().isEnum() && !f.getType().isAssignableFrom(si)) {
                f.setAccessible(true);
                autoFields.computeIfAbsent(si, k -> new HashSet<>()).add(new FieldNoLogging(f));
            }
        }
    }

    private static <T> JsonSerializer<T> getSerializer() {
        return (o, type, jsonSerializationContext) -> {
            JsonObject jObj = logSafeGson.toJsonTree(o).getAsJsonObject();
            maskFields.getOrDefault(o.getClass(), Collections.emptySet()).forEach(f -> {
                Object obj = f.getValue(o);
                if (obj instanceof Collection) {
                    JsonArray array = new JsonArray();
                    ((Collection<?>) obj).forEach(v -> array.add(f.getMaskedValue(v.toString())));
                    jObj.add(f.getName(), array);
                } else if (obj != null) {
                    jObj.addProperty(f.getName(), f.getMaskedValue(obj.toString()));
                }
            });

            autoFields.getOrDefault(o.getClass(), Collections.emptySet()).forEach(f -> {
                Object si = f.getValue(o);
                if (mayHasSensitiveInfo(si)) {
                    jObj.add(f.getName(), toJsonElement(si));
                } else if (si instanceof Collection) {
                    JsonArray array = new JsonArray();
                    ((Collection<?>) si).forEach(v -> array.add(toJsonElement(v)));
                    jObj.add(f.getName(), array);
                }
            });

            return jObj;
        };
    }

    public static JsonElement toJsonElement(Object o) {
        return logSafeGson.toJsonTree(o, getGsonType(o.getClass()));
    }

    public static String toJson(Object o) {
        return logSafeGson.toJson(o, getGsonType(o.getClass()));
    }

    public static boolean needMaskLog(Class clz) {
        return maskFields.keySet().contains(clz);
    }

    public static Map<String, String> getValuesToMask(Object o) {
        Map<String, String> results = new HashMap<>();
        maskFields.getOrDefault(o.getClass(), Collections.emptySet()).forEach(f -> {
            Object obj = f.getValue(o);
            if (obj instanceof Collection) {
                ((Collection<?>) obj).forEach(v -> results.put(v.toString(), f.getMaskedValue(v.toString())));
            } else if (obj != null) {
                results.put(obj.toString(), f.getMaskedValue(obj.toString()));
            }
        });

        autoFields.getOrDefault(o.getClass(), Collections.emptySet()).forEach(f -> {
            Object si = f.getValue(o);
            if (mayHasSensitiveInfo(si)) {
                results.putAll(getValuesToMask(si));
            } else if (si instanceof Collection) {
                ((Collection<?>) si).forEach(v -> {
                    if (mayHasSensitiveInfo(v)) {
                        results.put(v.toString(), f.getMaskedValue(v.toString()));
                    }
                });
            }
        });

        results.remove(Strings.EMPTY);
        return results;
    }

    public static void maskInventory(Object o) {
        maskFields.getOrDefault(o.getClass(), Collections.emptySet()).forEach(f -> {
            Object obj = f.getValue(o);
            if (obj != null) {
                String maskedValue = f.getMaskedValue(obj.toString());
                f.field.setAccessible(true);
                try {
                    f.field.set(o,maskedValue);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static Map<String, NoLogging.Type> getSensitiveFields(Class<?> clz) {
        Map<String, NoLogging.Type> paths = new HashMap<>();
        List<Field> fs = FieldUtils.getAllFields(clz);
        for (Field f : fs) {
            NoLogging an = f.getAnnotation(NoLogging.class);
            if (an != null && an.type() == NoLogging.Type.Simple) {
                paths.put(f.getName(), NoLogging.Type.Simple);
            } else if (mayHasSensitiveInfo(f.getType()) && !f.getType().isEnum() && !f.getType().isAssignableFrom(clz)) {
                String path = f.getName();
                getSensitiveFields(f.getType()).forEach((k, v) -> paths.put(path + '.' + k, v));
            }
        }

        return paths;
    }

    public static void registryTagHider(Function<String, String> hider) {
        tagInfoHider = hider;
    }

    private static boolean mayHasSensitiveInfo(Class<?> clz) {
        Package p = clz.getPackage();
        return p != null && p.getName().startsWith("org.zstack") && searchClasses.stream().anyMatch(it -> it.isAssignableFrom(clz));
    }

    private static boolean mayHasSensitiveInfo(Object obj) {
        return obj != null && mayHasSensitiveInfo(obj.getClass());
    }

    private static Class<?> getGsonType(Class<?> clz) {
        return searchClasses.stream().filter(it -> it.isAssignableFrom(clz)).findFirst().orElse(Object.class);
    }
}
