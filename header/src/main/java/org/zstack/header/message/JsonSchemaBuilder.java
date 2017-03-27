package org.zstack.header.message;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.GsonTransient;
import org.zstack.header.message.NoJsonSchema;
import org.zstack.utils.FieldUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2016/12/12.
 */
public class JsonSchemaBuilder {
    Object object;

    private LinkedHashMap<String, String> schema = new LinkedHashMap<>();

    public JsonSchemaBuilder(Object object) {
        this.object = object;
    }

    private boolean isSkip(Field f) {
        return f.isAnnotationPresent(NoJsonSchema.class) || Modifier.isStatic(f.getModifiers())
                || f.isAnnotationPresent(GsonTransient.class);
    }

    private void build(Object o, Stack<String> paths) throws IllegalAccessException {
        List<Field> fields = FieldUtils.getAllFields(o.getClass());

        for (Field f : fields) {
            if (isSkip(f)) {
                continue;
            }

            f.setAccessible(true);
            Object value = f.get(o);
            if (value == null) {
                // null value
                continue;
            }

            if (value.getClass().getCanonicalName().startsWith("java.")) {
                // for JRE classes, only deal with Collection and Map
                if (value instanceof Collection) {
                    Collection c = (Collection) value;

                    Class gtype = FieldUtils.getGenericType(f);

                    if (gtype != null && !gtype.getName().startsWith("java.")) {
                        int i = 0;
                        for (Object co : c) {
                            paths.push(String.format("%s[%s]", f.getName(), i++));
                            build(co, paths);
                            paths.pop();
                        }
                    }

                } else if (value instanceof Map) {
                    Class gtype = FieldUtils.getGenericType(f);

                    if (gtype != null && !gtype.getName().startsWith("java.")) {
                        for (Object me : ((Map) value).entrySet()) {
                            Map.Entry e = (Map.Entry) me;
                            paths.push(String.format("%s.%s", f.getName(), e.getKey().toString()));
                            build(e.getValue(), paths);
                            paths.pop();
                        }
                    }
                }

                // don't record standard JRE classes

            } else if (value.getClass().getCanonicalName().startsWith("org.zstack")) {
                paths.push(f.getName());
                build(value, paths);
                paths.pop();
            }
        }

        if (!paths.isEmpty()) {
            schema.put(StringUtils.join(paths, "."), o.getClass().getName());
        }
    }

    // support Map and org.zstack.* objects
    public Map<String, String> build() {
        try {
            if (!object.getClass().getName().startsWith("org.zstack") && !(object instanceof Map)) {
                throw new CloudRuntimeException(String.format("only a org.zstack.* object can be built schema, %s is not", object.getClass()));
            }

            if (object instanceof Map) {
                Map m = (Map) object;
                for (Object o : m.entrySet()) {
                    Map.Entry e = (Map.Entry) o;
                    if (e.getValue() == null) {
                        continue;
                    }

                    if (e.getValue().getClass().getName().startsWith("java.") &&
                            !Collection.class.isAssignableFrom(e.getValue().getClass())) {
                        continue;
                    }

                    if (Collection.class.isAssignableFrom(e.getValue().getClass())) {
                        Collection c = (Collection) e.getValue();
                        int i = 0;
                        for (Object it : c) {
                            Stack<String> path = new Stack<>();
                            path.add(String.format("%s[%s]", e.getKey(), i++));
                            build(it, path);
                        }
                    } else {
                        build(e.getValue(), new Stack<String>() {
                            {
                                add(e.getKey().toString());
                            }
                        });
                    }
                }
            } else {
                build(object, new Stack<>());
            }

            List<String> keys = new ArrayList<>(schema.keySet());
            Collections.reverse(keys);
            LinkedHashMap ret = new LinkedHashMap(schema.size());
            for (String key : keys) {
                ret.put(key, schema.get(key));
            }

            return ret;
        } catch (IllegalAccessException e) {
            throw new CloudRuntimeException(e);
        }
    }
}
