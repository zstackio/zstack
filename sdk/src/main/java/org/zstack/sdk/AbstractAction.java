package org.zstack.sdk;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by xing5 on 2016/12/9.
 */
public abstract class AbstractAction {
    public String apiId;

    abstract RestInfo getRestInfo();

    static class Parameter {
        Field field;
        Param annotation;
    }

    abstract Map<String, Parameter> getParameterMap();

    private void initializeParametersIfNot() {
        synchronized (getParameterMap()) {
            if (getParameterMap().isEmpty()) {
                List<Field> fields = getAllFields();

                for (Field f : fields) {
                    Param at = f.getAnnotation(Param.class);
                    if (at == null) {
                        continue;
                    }

                    Parameter p = new Parameter();
                    p.annotation = at;
                    p.field = f;
                    p.field.setAccessible(true);

                    getParameterMap().put(f.getName(), p);
                }
            }
        }
    }

    protected List<Field> getAllFields() {
        Class c = getClass();
        List<Field> fs = new ArrayList<>();
        while (c != Object.class) {
            Collections.addAll(fs, c.getDeclaredFields());
            c = c.getSuperclass();
        }

        return fs;
    }

    Set<String> getAllParameterNames() {
        initializeParametersIfNot();
        return getParameterMap().keySet();
    }

    Object getParameterValue(String name){
        return getParameterValue(name, true);
    }

    Object getParameterValue(String name, boolean exceptionOnNotFound){
        Parameter p = getParameterMap().get(name);
        if (p == null) {
            if (exceptionOnNotFound) {
                throw new ApiException(String.format("no such parameter[%s]", name));
            } else {
                return null;
            }
        }

        try {
            return p.field.get(this);
        } catch (IllegalAccessException e) {
            throw new ApiException(e);
        }
    }

    void checkParameters() {
        initializeParametersIfNot();

        try {
            for (Parameter p : getParameterMap().values()) {
                Object value = p.field.get(this);
                Param at = p.annotation;

                if (at.required() && value == null) {
                    throw new ApiException(String.format("missing mandatory field[%s]", p.field.getName()));
                }

                if (value != null && (value instanceof String) && !at.noTrim()) {
                    value = ((String) value).trim();
                    p.field.set(this, value);
                }

                if (value != null && at.maxLength() != Integer.MIN_VALUE && (value instanceof String)) {
                    String str = (String) value;
                    if (str.length() > at.maxLength()) {
                        throw new ApiException(String.format("filed[%s] exceeds the max length[%s chars] of string",
                                p.field.getName(), at.maxLength()));
                    }
                }

                if (value != null && at.minLength() != 0 && (value instanceof String)) {
                    String str = (String) value;
                    if (str.length() < at.minLength()) {
                        throw new ApiException(String.format("filed[%s] less than the min length[%s chars] of string",
                                p.field.getName(), at.minLength()));
                    }
                }

                if (value != null && at.validValues().length > 0) {
                    List<String> vals = new ArrayList<>();
                    for (String val: at.validValues()) {
                        vals.add(val.toLowerCase());
                    }
                    if (!vals.contains(value.toString().toLowerCase())) {
                        throw new ApiException(String.format("invalid value of the field[%s], valid values are %s",
                                p.field.getName(), vals));
                    }
                }

                if (value != null && at.validRegexValues() != null && at.validRegexValues().trim().equals("") == false) {
                    String regex = at.validRegexValues().trim();
                    Pattern pt = Pattern.compile(regex);
                    Matcher mt = pt.matcher(value.toString());
                    if (!mt.matches()) {
                        throw new ApiException(String.format("the value of the field[%s] doesn't match the required regular" +
                                " expression[%s]", p.field.getName(), regex));
                    }
                }

                if (value != null && at.nonempty() && value instanceof Collection) {
                    Collection col = (Collection) value;
                    if (col.isEmpty()) {
                        throw new ApiException(String.format("field[%s] cannot be an empty list", p.field.getName()));
                    }
                }

                if (value != null && !at.nullElements() && value instanceof Collection) {
                    Collection col = (Collection) value;
                    for (Object o : col) {
                        if (o == null) {
                            throw new ApiException(String.format("field[%s] cannot contain any null element", p.field.getName()));
                        }
                    }
                }

                if (value != null && !at.emptyString()) {
                    if ((value instanceof String) && ((String) value).length() == 0) {
                        throw new ApiException(String.format("the value of the field[%s] cannot be an empty string", p.field.getName()));
                    } else if (value instanceof Collection) {
                        for (Object v : (Collection) value) {
                            if (v instanceof String && ((String) v).length() == 0) {
                                throw new ApiException(String.format("the field[%s] cannot contain any empty string", p.field.getName()));
                            }
                        }
                    }
                }

                if (value != null && at.numberRange().length > 0
                        && (value instanceof Long || long.class.isAssignableFrom(value.getClass()) || value instanceof Integer || int.class.isAssignableFrom(value.getClass()))) {
                    long low = at.numberRange()[0];
                    long high = at.numberRange()[1];
                    long val = Long.valueOf(((Number) value).longValue());
                    if (val < low || val > high) {
                        throw new ApiException(String.format("the value of the field[%s] out of range[%s, %s]", p.field.getName(), low, high));
                    }
                }
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(e);
        }
    }
}
