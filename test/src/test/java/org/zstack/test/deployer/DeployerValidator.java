package org.zstack.test.deployer;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.lang.reflect.Field;
import java.util.Collection;

// xsd spec 1.0 has a flaw that there is no way to get an any order sequence,
// which means you have to arrange xml elements as the same order as they present
// in xsd schema file if using JAXB validator. So we have to write our validator here
public class DeployerValidator {
    private static final CLogger logger = Utils.getLogger(DeployerValidator.class);
    private DeployerConfig config;

    DeployerValidator(DeployerConfig config) {
        this.config = config;
    }

    private void validateCollection(Field f, Object obj) throws IllegalArgumentException, IllegalAccessException {
        XmlType xtype = obj.getClass().getAnnotation(XmlType.class);
        if (xtype == null) {
            return;
        }
        String elementName = xtype.name();
        logger.debug(String.format("validating %s->%s", elementName, f.getName()));

        Collection l = (Collection) f.get(obj);
        XmlElement eat = f.getAnnotation(XmlElement.class);
        if (eat != null && (eat.required() && (l == null || l.isEmpty()))) {
            throw new IllegalArgumentException(String.format("field[%s] of element[%s] is mandatory, cannot be missed", f.getName(), elementName));
        }
        XmlAttribute aat = f.getAnnotation(XmlAttribute.class);
        if (aat != null && (aat.required() && (l == null || l.isEmpty()))) {
            throw new IllegalArgumentException(String.format("field[%s] of element[%s] is mandatory, cannot be missed", aat.name(), elementName));
        }

        if (l != null) {
            Object val = l.iterator().next();
            if (val != null) {
                validateObject(val);
            }
        }
    }

    private void validateField(Field f, Object obj) throws IllegalArgumentException, IllegalAccessException {
        XmlType xtype = obj.getClass().getAnnotation(XmlType.class);
        if (xtype == null) {
            return;
        }

        Object val = f.get(obj);
        String elementName = xtype.name();
        logger.debug(String.format("validating %s->%s", elementName, f.getName()));

        XmlElement eat = f.getAnnotation(XmlElement.class);
        if (eat != null && eat.required() && val == null) {
            throw new IllegalArgumentException(String.format("field[%s] of element[%s] is mandatory, cannot be missed", f.getName(), elementName));
        }

        XmlAttribute aat = f.getAnnotation(XmlAttribute.class);
        if (aat != null && aat.required() && val == null) {
            throw new IllegalArgumentException(String.format("field[%s] of element[%s] is mandatory, cannot be missed", aat.name(), elementName));
        }

        if (val != null) {
            validateObject(val);
        }
    }

    private void validateObject(Object obj) throws IllegalArgumentException, IllegalAccessException {
        for (Field f : obj.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            if (Collection.class.isAssignableFrom(f.getType())) {
                validateCollection(f, obj);
            } else {
                validateField(f, obj);
            }
        }
    }

    void vaildate() {
        try {
            validateObject(config);
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }
}
