package org.zstack.expon.sdk;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by xing5 on 2016/12/9.
 */
public abstract class ExponRequest {
    @Param
    String sessionId;

    long timeout = -1;

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public static class Parameter {
        Field field;
        Param annotation;
    }

    // API parameter
    protected abstract Map<String, Parameter> getParameterMap();

    // TODO
    public void initializeParametersIfNot() {
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
        return getParameterValue(getParameterMap(), name, exceptionOnNotFound);
    }

    Object getParameterValue(Map<String, Parameter> map, String name, boolean exceptionOnNotFound){
        Parameter p = map.get(name);
        if (p == null) {
            if (exceptionOnNotFound) {
                throw new ExponApiException(String.format("no such parameter[%s]", name));
            } else {
                return null;
            }
        }

        try {
            return p.field.get(this);
        } catch (IllegalAccessException e) {
            throw new ExponApiException(e);
        }
    }

    // TODO
    public void checkParameters() {
        initializeParametersIfNot();

        try {
            boolean useEncryptParam = checkIfUseEncryptParam();
            for (Parameter p : getParameterMap().values()) {
                Object value = p.field.get(this);
                Param at = p.annotation;

                if (!useEncryptParam && at.required() && value == null) {
                    throw new ExponApiException(String.format("missing mandatory field[%s]", p.field.getName()));
                }

                if (value != null && at.validValues().length > 0) {
                    if (value instanceof Collection) {
                        for (Object v : (Collection) value) {
                            validateValue(at.validValues(), v.toString(), p.field.getName());
                        }
                    } else {
                        validateValue(at.validValues(), value.toString(), p.field.getName());
                    }
                }
            }
        } catch (ExponApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ExponApiException(e);
        }
    }

    @SuppressWarnings("rawtypes")
    private boolean checkIfUseEncryptParam() {
        Object systemTags = getParameterValue("systemTags", false);
        if (!(systemTags instanceof Collection) || ((Collection) systemTags).isEmpty()) {
            return false;
        }
        for (Object val: (Collection) systemTags) {
            if (val.toString().startsWith("encryptionParam::")) {
                return true;
            }
        }
        return false;
    }

    private static void validateValue(String[] validValues, String value, String fieldName) {
        List<String> vals = new ArrayList<>();
        for (String val: validValues) {
            vals.add(val.toLowerCase());
        }
        if (!vals.contains(value.toLowerCase())) {
            throw new ExponApiException(String.format("invalid value of the field[%s], valid values are %s",
                    fieldName, vals));
        }
    }
}
