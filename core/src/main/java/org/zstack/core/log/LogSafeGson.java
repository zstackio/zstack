package org.zstack.core.log;

import com.google.gson.*;
import org.apache.logging.log4j.util.Strings;
import org.springframework.security.access.method.P;
import org.zstack.header.log.HasSensitiveInfo;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.GsonTransient;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.GsonUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by MaJin on 2019/9/21.
 */
public class LogSafeGson {
    private static Map<Class, Set<FieldNoLogging>> maskFields = new HashMap<>();
    private static Map<Class, Set<FieldNoLogging>> autoFields = new HashMap<>();

    private static class FieldNoLogging {
        Field field;
        NoLogging annotation;

        private static Pattern uriPattern = Pattern.compile(":[^:]*@");

        FieldNoLogging(Field field) {
            this.field = field;
        }

        FieldNoLogging(Field field, NoLogging annotation) {
            this.field = field;
            this.annotation = annotation;
        }

        String getName() {
            return field.getName();
        }

        Object getValue(HasSensitiveInfo obj) {
            try {
                return field.get(obj);
            } catch (IllegalAccessException e) {
                return null;
            }
        }

        String getMaskedValue(String raw) {
            if (annotation.type().simple()) {
                return "*****";
            } else {
                return Utils.getLogMaskWords().getOrDefault(raw, uriPattern.matcher(raw).replaceFirst(":*****@"));
            }
        }
    }

    static {
        for (Class<? extends HasSensitiveInfo> si : BeanUtils.reflections.getSubTypesOf(HasSensitiveInfo.class)) {
            List<Field> noLogFs = FieldUtils.getAnnotatedFields(NoLogging.class, si);
            for (Field f : noLogFs) {
                f.setAccessible(true);
                NoLogging an = f.getAnnotation(NoLogging.class);
                if (an.behavior().auto()) {
                    autoFields.computeIfAbsent(si, k -> new HashSet<>()).add(new FieldNoLogging(f, an));
                } else {
                    maskFields.computeIfAbsent(si, k -> new HashSet<>()).add(new FieldNoLogging(f, an));
                }
            }

            List<Field> siFs = FieldUtils.getDeclaringClassFields(HasSensitiveInfo.class, si);
            for (Field f : siFs) {
                if (!f.isAnnotationPresent(NoLogging.class)) {
                    f.setAccessible(true);
                    autoFields.computeIfAbsent(si, k -> new HashSet<>()).add(new FieldNoLogging(f));
                }
            }
        }
    }

    private static Gson logSafeGson = new GsonUtil().setInstanceCreator(HasSensitiveInfo.class, new JsonSerializer<HasSensitiveInfo>() {

        @Override
        public JsonElement serialize(HasSensitiveInfo o, Type type, JsonSerializationContext jsonSerializationContext) {
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
                if (si instanceof HasSensitiveInfo) {
                    jObj.add(f.getName(), logSafeGson.toJsonTree(si, HasSensitiveInfo.class));
                } else if (si instanceof Collection) {
                    JsonArray array = new JsonArray();
                    ((Collection<?>) si).forEach(v -> {
                        if (v instanceof HasSensitiveInfo) {
                            array.add(logSafeGson.toJsonTree(si, HasSensitiveInfo.class));
                        } else {
                            array.add(logSafeGson.toJsonTree(si));
                        }
                    });
                    jObj.add(f.getName(), array);
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

    public static Map<String, String> getValuesToMask(HasSensitiveInfo o) {
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
            if (si instanceof HasSensitiveInfo) {
                results.putAll(getValuesToMask((HasSensitiveInfo) si));
            } else if (si instanceof Collection) {
                ((Collection<?>) si).forEach(v -> {
                    if (v instanceof HasSensitiveInfo) {
                        results.put(v.toString(), f.getMaskedValue(v.toString()));
                    }
                });
            }
        });

        results.remove(Strings.EMPTY);
        return results;
    }
}
