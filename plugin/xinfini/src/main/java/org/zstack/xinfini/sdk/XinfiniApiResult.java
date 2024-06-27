package org.zstack.xinfini.sdk;

import org.apache.commons.beanutils.PropertyUtils;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.operr;

public class XinfiniApiResult {
    private static final CLogger logger = Utils.getLogger(XinfiniApiResult.class);
    private String resultString;
    private String message;
    private int returnCode;

    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setResultString(String resultString) {
        this.resultString = resultString;
    }

    private static Object getProperty(Object bean, Iterator<String> it) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        String path = it.next();
        if (bean instanceof Map) {
            Pattern re = Pattern.compile("(.*)\\[(\\d+)]");
            Matcher m = re.matcher(path);
            if (m.find()) {
                path = String.format("(%s)[%s]", m.group(1), m.group(2));
            }
        }

        Object val = PropertyUtils.getProperty(bean, path);

        if (it.hasNext()) {
            return getProperty(val, it);
        } else {
            return val;
        }
    }

    public static Object getProperty(Object bean, String path) {
        List<String> paths = asList(path.split("\\."));
        try {
            return getProperty(bean, paths.iterator());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setProperty(Object bean, Iterator<String> it, String fieldName, Object val) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        if (it.hasNext()) {
            bean = getProperty(bean, it);
        }

        if (bean instanceof Map) {
            Pattern re = Pattern.compile("(.*)\\[(\\d+)]");
            Matcher m = re.matcher(fieldName);
            if (m.find()) {
                fieldName = String.format("(%s)[%s]", m.group(1), m.group(2));
            }
        }

        PropertyUtils.setProperty(bean, fieldName, val);
    }

    public static void setProperty(Object bean, String path, Object val) {
        List<String> paths = asList(path.split("\\."));
        String fieldName = paths.get(paths.size()-1);
        paths = paths.subList(0, paths.size()-1);

        try {
            setProperty(bean, paths.iterator(), fieldName, val);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public  <T> T getResult(Class<T> clz) {
        if (resultString == null || resultString.isEmpty()) {
            return null;
        }
        T ret;
        try {
            ret = XInfiniClient.gson.fromJson(resultString, clz);
        } catch (Exception e) {
            throw new OperationFailureException(operr("format api result to class[%s] failed, resultString: %s, exception: %s", clz.getName(), resultString, e.getMessage()));
        }
        return ret;
    }

    public String getResultString() {
        return resultString;
    }
}
