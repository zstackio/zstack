package org.zstack.header.message;

import org.springframework.util.StringUtils;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.rest.RestResponse;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.TypeUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;


public abstract class APIMessage extends NeedReplyMessage {
    /**
     * @ignore
     */
    @NoJsonSchema
    @APINoSee
    private SessionInventory session;

    public SessionInventory getSession() {
        return session;
    }

    public void setSession(SessionInventory session) {
        this.session = session;
    }

    public static class FieldParam {
        public Field field;
        public APIParam param;
    }

    @NoJsonSchema
    @APINoSee
    @GsonTransient
    public static Map<Class, Collection<FieldParam>> apiParams = new HashMap<>();

    @NoJsonSchema
    @APINoSee
    @GsonTransient
    public static Set<Class> apiMessageClasses = BeanUtils.reflections.getSubTypesOf(APIMessage.class)
            .stream().filter(c -> !Modifier.isStatic(c.getModifiers()) && c.isAnnotationPresent(RestRequest.class))
            .collect(Collectors.toSet());

    static {
        collectApiParams();
        collectResponseMappingFields();
    }

    private static void collectResponseMappingFields() {
        BeanUtils.reflections.getSubTypesOf(APIResponse.class).stream().filter(clz-> clz != APIReply.class && clz != APIEvent.class
         && !Modifier.isStatic(clz.getModifiers()) && clz.isAnnotationPresent(RestResponse.class)).forEach(clz -> {
            RestResponse annotation = clz.getAnnotation(RestResponse.class);
            Map responseMappingFields = new HashMap();

            if (!annotation.allTo().equals("")) {
                responseMappingFields.put(annotation.allTo(), annotation.allTo());
            } else if (annotation.fieldsTo().length > 0) {
                if (annotation.fieldsTo().length == 1 && "all".equals(annotation.fieldsTo()[0])) {
                    List<Field> apiFields = FieldUtils.getAllFields(clz);
                    apiFields = apiFields.stream().filter(f -> !f.isAnnotationPresent(APINoSee.class) && !Modifier.isStatic(f.getModifiers())).collect(Collectors.toList());

                    for (Field f : apiFields) {
                        responseMappingFields.put(f.getName(), f.getName());
                    }
                } else {
                    for (String mf : annotation.fieldsTo()) {
                        String[] kv = mf.split("=");
                        if (kv.length == 2) {
                            responseMappingFields.put(kv[0].trim(), kv[1].trim());
                        } else if (kv.length == 1) {
                            responseMappingFields.put(kv[0].trim(), kv[0].trim());
                        } else {
                            throw new CloudRuntimeException(String.format("bad mappingFields[%s] of %s", mf, clz));
                        }

                    }
                }
            }

            APIResponse.responseMappingFields.put(clz, responseMappingFields);
        });
    }

    public static Map<Class, Collection<FieldParam>> getApiParams() {
        return apiParams;
    }

    private static void collectApiParams() {
        class Nothing { @APIParam(required = false) public String nothing;}
        APIParam defaultAnnotation;
        try {
            defaultAnnotation = Nothing.class.getField("nothing").getAnnotation(APIParam.class);
        } catch (NoSuchFieldException e) {
            throw new CloudRuntimeException(e);
        }

        for (Class clz : apiMessageClasses) {
            List<Field> fields = FieldUtils.getAllFields(clz);

            Map<String, FieldParam> fmap = new HashMap<>();
            for (Field f : fields) {
                if (Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                if (f.isAnnotationPresent(APINoSee.class)) {
                    continue;
                }
                if (Modifier.isTransient(f.getModifiers())) {
                    continue;
                }
                if (f.isAnnotationPresent(GsonTransient.class)) {
                    continue;
                }

                APIParam at = f.getAnnotation(APIParam.class);
                if (at == null) {
                    at = defaultAnnotation;
                }

                f.setAccessible(true);
                FieldParam fp = new FieldParam();
                fp.field = f;
                fp.param = at;
                fmap.put(f.getName(), fp);
            }

            OverriddenApiParams at = (OverriddenApiParams) clz.getAnnotation(OverriddenApiParams.class);
            if (at != null) {
                for (OverriddenApiParam atp : at.value()) {
                    Field f = FieldUtils.getField(atp.field(), clz);
                    if (f == null) {
                        throw new CloudRuntimeException(String.format("cannot find the field[%s] specified in @OverriddenApiParam of class[%s]",
                                atp.field(), clz));
                    }

                    FieldParam fp = new FieldParam();
                    fp.field = f;
                    fp.param = atp.param();
                    fmap.put(atp.field(), fp);
                }
            }

            apiParams.put(clz, fmap.values());
        }
    }

    public static class InvalidApiMessageException extends RuntimeException {
        private Object[] arguments = new Object[]{};

        public InvalidApiMessageException(Object[] arguments) {
            this.arguments = arguments;
        }

        public InvalidApiMessageException(String message, Object...arguments) {
            super(message);
            this.arguments = arguments;
        }

        public InvalidApiMessageException(String message, Throwable cause, Object[] arguments) {
            super(message, cause);
            this.arguments = arguments;
        }

        public InvalidApiMessageException(Throwable cause, Object[] arguments) {
            super(cause);
            this.arguments = arguments;
        }

        public InvalidApiMessageException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, Object[] arguments) {
            super(message, cause, enableSuppression, writableStackTrace);
            this.arguments = arguments;
        }

        public Object[] getArguments() {
            return arguments;
        }

        public void setArguments(Object[] arguments) {
            this.arguments = arguments;
        }
    }


    public void validate() throws IllegalAccessException {
        validate(null);
    }

    public void validate(ApiMessageValidator validator) throws IllegalAccessException {
        Collection<FieldParam> params = apiParams.get(this.getClass());
        if (params == null) {
            throw new CloudRuntimeException(String.format("cannot find ApiParams for the class[%s]", this.getClass()));
        }

        for (FieldParam fp : params) {
            Field f = fp.field;
            final APIParam at = fp.param;

            f.setAccessible(true);
            Object value = f.get(this);

            if (value != null && (value instanceof String) && !at.noTrim()) {
                value = ((String) value).trim();
                f.set(this, value);
            }

            if (value != null && at.maxLength() != Integer.MIN_VALUE && (value instanceof String)) {
                String str = (String) value;
                if (str.length() > at.maxLength()) {
                    throw new InvalidApiMessageException("field[%s] of message[%s] exceeds max length of string. expected was <= %s, actual was %s",
                            f.getName(), getClass().getName(), at.maxLength(), str.length());
                }
            }

            if (value != null && at.minLength() != 0 && (value instanceof String)) {
                String str = (String) value;
                if (str.length() < at.minLength()) {
                    throw new InvalidApiMessageException("field[%s] of message[%s] less than the min length of string. expected was >= %s, actual was %s",
                            f.getName(), getClass().getName(), at.minLength(), str.length());
                }
            }

            if (at.required() && value == null) {
                throw new InvalidApiMessageException("field[%s] of message[%s] is mandatory, can not be null", f.getName(), getClass().getName());
            }

            if (value != null && at.validValues().length > 0) {
                boolean found = false;
                for (String val : at.validValues()) {
                    if (val.equals(value.toString())) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    throw new InvalidApiMessageException("valid value for field[%s] of message[%s] are %s, but %s found", f.getName(),
                            getClass().getName(), asList(at.validValues()), value);
                }
            }

            if (value != null && at.validRegexValues() != null && at.validRegexValues().trim().equals("") == false) {
                String regex = at.validRegexValues().trim();
                Pattern p = Pattern.compile(regex);
                Matcher mt = p.matcher(value.toString());
                if (!mt.matches()){
                    throw new InvalidApiMessageException("valid regex value for field[%s] of message[%s] are %s, but %s found", f.getName(),
                            getClass().getName(), regex, value);
                }
            }

            if (value !=null && at.nonempty() && value instanceof Collection) {
                Collection col = (Collection) value;
                if (col.isEmpty()) {
                    throw new InvalidApiMessageException("field[%s] must be a nonempty list", f.getName());
                }
            }

            if (value !=null && !at.nullElements() && value instanceof Collection) {
                Collection col = (Collection) value;
                for (Object o : col) {
                    if (o == null) {
                        throw new InvalidApiMessageException("field[%s] cannot contain a NULL element", f.getName());
                    }
                }
            }

            if (value != null &&!at.emptyString()) {
                if (value instanceof String && StringUtils.isEmpty((String) value)) {
                    throw new InvalidApiMessageException("field[%s] cannot be an empty string", f.getName());
                } else if (value instanceof Collection) {
                    for (Object v : (Collection)value) {
                        if (v instanceof String && StringUtils.isEmpty((String)v)) {
                            throw new InvalidApiMessageException("field[%s] cannot contain any empty string", f.getName());
                        }
                    }
                }
            }

            if (value != null && at.numberRange().length > 0 && TypeUtils.isTypeOf(value, Integer.TYPE, Integer.class, Long.TYPE, Long.class)) {
                DebugUtils.Assert(at.numberRange().length == 2, String.format("invalid field[%s], APIParam.numberRange must have and only have 2 items", f.getName()));
                long low = at.numberRange()[0];
                long high = at.numberRange()[1];
                long val = Long.valueOf(((Number) value).longValue());
                if (val < low || val > high) {
                    if (at.numberRangeUnit().length > 0) {
                        DebugUtils.Assert(at.numberRangeUnit().length == 2, String.format("invalid field[%s], APIParam.numberRangeUnit must have and only have 2 items", f.getName()));
                        String lowUnit = at.numberRangeUnit()[0];
                        String highUnit = at.numberRangeUnit()[1];
                        throw new InvalidApiMessageException("field[%s] must be in range of [%s %s, %s %s]", f.getName(), low, lowUnit, high, highUnit);
                    } else {
                        throw new InvalidApiMessageException("field[%s] must be in range of [%s, %s]", f.getName(), low, high);
                    }
                }
            }

            if (validator != null) {
                validator.validate(this, f, value, at);
            }
        }
    }
}
