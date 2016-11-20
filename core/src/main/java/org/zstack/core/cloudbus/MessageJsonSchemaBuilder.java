package org.zstack.core.cloudbus;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.GsonTransient;
import org.zstack.header.message.NeedJsonSchema;
import org.zstack.header.message.NoJsonSchema;
import org.zstack.header.search.Inventory;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.TypeUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 */
public class MessageJsonSchemaBuilder {
    private static Map<Field, Field> skipMap = new ConcurrentHashMap<Field, Field>();

    private static boolean isSkip(Field f) {
        if (skipMap.containsKey(f)) {
            return true;
        }

        if (TypeUtils.isPrimitiveOrWrapper(f.getType())) {
            skipMap.put(f, f);
            return true;
        }

        if (f.isAnnotationPresent(NoJsonSchema.class)) {
            skipMap.put(f, f);
            return true;
        }

        if (Modifier.isStatic(f.getModifiers())) {
            skipMap.put(f, f);
            return true;
        }

        if (f.isAnnotationPresent(GsonTransient.class)) {
            skipMap.put(f, f);
            return true;
        }

        return false;
    }

    private static Object getValue(Field f, Object obj) throws IllegalAccessException {
        f.setAccessible(true);
        return f.get(obj);
    }

    private static boolean isNullValue(Field f, Object obj) throws IllegalAccessException {
        return getValue(f, obj) == null;
    }

    private static void buildSchema(Object obj, Map<String, List<String>> schema, Stack<String> trace, Stack<Object> paths) throws IllegalAccessException {
        List<Field> fs = FieldUtils.getAllFields(obj.getClass());
        for (Field f : fs) {
            if (isSkip(f)) {
                continue;
            }

            if (Map.class.isAssignableFrom(f.getType())) {
                schemaMap(f, obj, schema, trace, paths);
                continue;
            }

            if (Collection.class.isAssignableFrom(f.getType())) {
                Class genericType = FieldUtils.getGenericType(f);
                if (genericType != null && TypeUtils.isPrimitiveOrWrapper(genericType)) {
                    continue;
                }

                if (!List.class.isAssignableFrom(f.getType())) {
                    throw new CloudRuntimeException(String.format("the collection type in message can only be List, but %s.%s is %s",
                            f.getDeclaringClass().getName(), f.getName(), f.getType().getName()));
                }

                schemaList(f, obj, schema, trace, paths);
                continue;
            }

            schemaObject(f, obj, schema, trace, paths);
        }
    }

    private static void schemaList(Field f, Object obj, Map<String, List<String>> schema, Stack<String> trace, Stack<Object> paths) throws IllegalAccessException {
        if (isNullValue(f, obj)) {
            return;
        }

        Object value = getValue(f, obj);
        if (paths.contains(value)) {
            paths.push(value);
            throw new CloudRuntimeException(String.format("recursive object graph: %s", StringUtils.join(paths, " --> ")));
        }

        paths.push(value);
        List col = (List) value;
        for (Object item : col) {
            String itemName = String.format("%s[%s]", f.getName(), col.indexOf(item));
            if (isObjectNeedSchema(item)) {
                addToSchema(item.getClass(), itemName, schema, trace);
            }

            trace.push(itemName);
            buildSchema(item, schema, trace, paths);
            trace.pop();
        }

        paths.pop();
    }

    public static Map<String, List<String>> buildSchema(Object msg) {
        try {
            Stack<Object> paths = new Stack<Object>();
            Stack<String> trace = new Stack<String>();
            Map<String, List<String>> schema = new LinkedHashMap<String, List<String>>();

            buildSchema(msg, schema, trace, paths);

            return schema;
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    private static void schemaObject(Field f, Object obj, Map<String, List<String>> schema, Stack<String> trace, Stack<Object> paths) throws IllegalAccessException {
        if (isNullValue(f, obj)) {
            return;
        }

        Object value = getValue(f, obj);
        if (paths.contains(value)) {
            paths.push(value);
            throw new CloudRuntimeException(String.format("recursive object graph: %s", StringUtils.join(paths, " --> ")));
        }


        if (isObjectNeedSchema(value)) {
            addToSchema(value.getClass(), f.getName(), schema, trace);
        }

        paths.push(value);
        trace.push(f.getName());
        buildSchema(value, schema, trace, paths);
        trace.pop();
        paths.pop();
    }

    private static void addToSchema(Class<?> realClass, String name, Map<String, List<String>> schema, Stack<String> trace) {
        String base = StringUtils.join(trace, ".");
        List<String> path = schema.get(realClass.getName());
        if (path == null) {
            path = new ArrayList<String>();
            schema.put(realClass.getName(), path);
        }
        if (base.equals("")) {
            path.add(name);
        } else {
            path.add(String.format("%s.%s", base, name));
        }
    }

    private static boolean isObjectNeedSchema(Object obj) {
        return obj != null && (obj.getClass().isAnnotationPresent(Inventory.class) || obj.getClass().isAnnotationPresent(NeedJsonSchema.class));
    }

    private static void schemaMap(Field f, Object obj, Map<String, List<String>> schema, Stack<String> trace, Stack<Object> paths) throws IllegalAccessException {
        Class genericType = FieldUtils.getGenericType(f);
        if (genericType != null && TypeUtils.isPrimitiveOrWrapper(genericType)) {
            return;
        }

        if (isNullValue(f, obj)) {
            return;
        }

        Object value = getValue(f, obj);
        if (paths.contains(value)) {
            paths.push(value);
            throw new CloudRuntimeException(String.format("recursive object graph: %s", StringUtils.join(paths, " --> ")));
        }

        paths.push(obj);
        Map map = (Map) value;
        Iterator<Entry> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            String key = e.getKey().toString();
            Object item = e.getValue();
            String itemName = String.format("%s[\"%s\"]", f.getName(), key);
            if (isObjectNeedSchema(item)) {
                addToSchema(item.getClass(), itemName, schema, trace);
            }

            trace.push(itemName);
            buildSchema(item, schema, trace, paths);
            trace.pop();
        }
        paths.pop();
    }
}
