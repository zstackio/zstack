package org.zstack.core.propertyvalidator;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.zstack.utils.BeanUtils.reflections;

public class ValidatorTool {
    private static final CLogger logger = CLoggerImpl.getLogger(org.zstack.core.propertyvalidator.ValidatorTool.class);
    static Map<Class<?>, Class<?>> validateMap = new HashMap<>();
    static Map<GlobalProperty, Field> annotationsAndFieldMap = new HashMap<>();
    static Map<Field,Annotation[]>fieldAndAnnotationsMap = new HashMap<>();

    static {
        Set<Class<?>> validateClasses = reflections.getTypesAnnotatedWith(ValidateTarget.class);
        for (Class<?> clz : validateClasses) {
            ValidateTarget at = clz.getAnnotation(ValidateTarget.class);
            if (at == null) {
                continue;
            }
            validateMap.put(at.target(), clz);
        }

        Set<Class<?>> clzs = reflections.getTypesAnnotatedWith(GlobalPropertyDefinition.class);
        for (Class<?> clz : clzs) {
            for (Field f : clz.getDeclaredFields()) {
                if (f.isSynthetic()) {
                    continue;
                }
                GlobalProperty at = f.getAnnotation(GlobalProperty.class);
                Annotation[] annotations = f.getAnnotations();
                fieldAndAnnotationsMap.put(f,annotations);
                annotationsAndFieldMap.put(at, f);
            }
        }
    }

    public boolean checkProperty(String propertyName, String propertyValue) {
        boolean result = false;

        Set<GlobalProperty> annotationsSet = annotationsAndFieldMap.keySet();
        for (GlobalProperty at : annotationsSet) {
            Field f = annotationsAndFieldMap.get(at);
            if (!at.name().trim().equals(propertyName)) {
                continue;
            }
            result = matchAnnotationMethod(f, propertyName, propertyValue);
        }
        return result;
    }

    private boolean matchAnnotationMethod(Field f, String propertyName, String propertyValue) {
        boolean result = false;
        Annotation[] annotations = fieldAndAnnotationsMap.get(f);
        for (Annotation annotation : annotations) {
            Class<?> annotationType = annotation.annotationType();
            Class<?> validatorType = validateMap.get(annotationType);

            if (validatorType == null) {
                continue;
            }
            try {
                Method annotationMethod = annotationType.getMethod("value");
                Object rule = annotationMethod.invoke(annotation);
                Constructor<? extends GlobalPropertyValidator> constructor = (Constructor<? extends GlobalPropertyValidator>) validatorType.getConstructor();
                GlobalPropertyValidator validator = constructor.newInstance();
                result = validator.validate(propertyName, propertyValue, rule);
            } catch (GlobalPropertyValidatorExecption t) {
                logger.warn("error happened :", t);
                throw new CloudRuntimeException(t);
            } catch (NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
