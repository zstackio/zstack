package org.zstack.externalStorage.sdk;

import java.lang.reflect.Field;
import java.util.*;

public interface ExternalStorageParam {
    class Parameter {
        Field field;
        Param annotation;
    }

    // API parameter
    Map<String, Parameter> getParameterMap();

    // TODO
    default void initializeParametersIfNot() {
        synchronized (getParameterMap()) {
            if (getParameterMap().isEmpty()) {
                List<Field> fields = getAllFields();

                for (Field f : fields) {
                    Param at = f.getAnnotation(Param.class);
                    if (at == null) {
                        continue;
                    }

                    Parameter p = new Parameter();
                    p.field = f;
                    p.field.setAccessible(true);



                    /*
                    NonAPIParam nonAPIParamAnnotation = f.getAnnotation(NonAPIParam.class);

                    if(at == null){
                        if(nonAPIParamAnnotation != null){
                            getNonAPIParameterMap().put(f.getName(), p);
                        }
                        continue;
                    }
                    */

                    p.annotation = at;

                    getParameterMap().put(f.getName(), p);
                }
            }
        }
    }

    default List<Field> getAllFields() {
        Class c = getClass();
        List<Field> fs = new ArrayList<>();
        while (c != Object.class) {
            Collections.addAll(fs, c.getDeclaredFields());
            c = c.getSuperclass();
        }

        return fs;
    }

    default Set<String> getAllParameterNames() {
        initializeParametersIfNot();
        return getParameterMap().keySet();
    }

    default Object getParameterValue(String name){
        return getParameterValue(name, true);
    }

    default Object getParameterValue(String name, boolean exceptionOnNotFound){
        return getParameterValue(getParameterMap(), name, exceptionOnNotFound);
    }

    default Object getParameterValue(Map<String, Parameter> map, String name, boolean exceptionOnNotFound){
        Parameter p = map.get(name);
        if (p == null) {
            if (exceptionOnNotFound) {
                throw new ExternalStorageApiException(String.format("no such parameter[%s]", name));
            } else {
                return null;
            }
        }

        try {
            return p.field.get(this);
        } catch (IllegalAccessException e) {
            throw new ExternalStorageApiException(e);
        }
    }

    default void checkParameters() {
        initializeParametersIfNot();

        try {
            for (Parameter p : getParameterMap().values()) {
                Object value = p.field.get(this);
                Param at = p.annotation;

                if (at.required() && value == null) {
                    throw new ExternalStorageApiException(String.format("missing mandatory field[%s]", p.field.getName()));
                }

                if (value == null) {
                    continue;
                }

                if (value instanceof ExternalStorageParam) {
                    ((ExternalStorageParam) value).checkParameters();
                }

                if (at.validValues().length > 0) {
                    if (value instanceof Collection) {
                        for (Object v : (Collection) value) {
                            validateValue(at.validValues(), v.toString(), p.field.getName());
                        }
                    } else {
                        validateValue(at.validValues(), value.toString(), p.field.getName());
                    }
                }

                if (at.numberRange().length > 0 && value instanceof Number) {
                    if (((Number) value).intValue() == 0) {
                        if (!at.nonempty()) {
                            continue;
                        }
                    }

                    validateNumberRange(at.numberRange(), (Number) value, p.field.getName());
                }
            }
        } catch (ExternalStorageApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalStorageApiException(e);
        }
    }

    static void validateValue(String[] validValues, String value, String fieldName) {
        List<String> vals = new ArrayList<>();
        for (String val: validValues) {
            vals.add(val.toLowerCase());
        }
        if (!vals.contains(value.toLowerCase())) {
            throw new ExternalStorageApiException(String.format("invalid value of the field[%s], valid values are %s",
                    fieldName, vals));
        }
    }

    static void validateNumberRange(long[] numberRange, Number value, String name) {
        if (value.longValue() < numberRange[0] || value.longValue() > numberRange[1]) {
            throw new ExternalStorageApiException(String.format("invalid value of the field[%s], valid range is [%s, %s]",
                    name, numberRange[0], numberRange[1]));
        }
    }
}
