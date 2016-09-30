package org.zstack.core.inventory;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.zstack.header.Component;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.search.Inventory;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 10:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class InventoryFacadeImpl implements InventoryFacade, Component {
    private class Info {
        Inventory inventory;
        Class<?> inventoryClass;
    };

    private Map<Class<?>, Info> inventoryMapping = new HashMap<Class<?>, Info>();

    private Info getInfo(Class<?> voClass) {
        Info info = inventoryMapping.get(voClass);
        if (info == null) {
            throw new IllegalArgumentException(String.format("Cannot find Inventory for class[%s], check if its responding Inventory is annotated by @Inventory ", voClass.getName()));
        }
        return info;
    }

    @Override
    public Object valueOf(Object vo) {
        return convertVOToInventory(vo);
    }

    @Override
    public List valueOf(Collection vos) {
        return convertVOsToInventories(vos);
    }

    @Override
    public boolean start() {
        try {
            ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
            scanner.addIncludeFilter(new AnnotationTypeFilter(Inventory.class));
            scanner.addExcludeFilter(new AnnotationTypeFilter(Controller.class));
            scanner.addExcludeFilter(new AnnotationTypeFilter(org.springframework.stereotype.Component.class));
            for (String pkg : getBasePkgNames()) {
                for (BeanDefinition bd : scanner.findCandidateComponents(pkg)) {
                    Class<?> inventoryClass = Class.forName(bd.getBeanClassName());
                    Inventory invat = inventoryClass.getAnnotation(Inventory.class);
                    Info info = new Info();
                    info.inventory = invat;
                    info.inventoryClass = inventoryClass;
                    inventoryMapping.put(invat.mappingVOClass(), info);
                }
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    public List<String> getBasePkgNames() {
        List<String> pkgNames = new ArrayList<String>();
        pkgNames.add("org.zstack");
        return pkgNames;
    }

    private Method getValueOfMethod(Class<?> inventoryClass, Class<?> voClass) throws NoSuchMethodException, SecurityException {
        Method valueOf = inventoryClass.getMethod("valueOf", voClass);
        return valueOf;
    }

    private Method getCollectionValueOfMethod(Inventory invat, Class<?> inventoryClass) throws NoSuchMethodException, SecurityException {
        String methodName = invat.collectionValueOfMethod();
        if (methodName.equals("")) {
            methodName = "valueOf";
        }
        Method valueOf = inventoryClass.getMethod(methodName, Collection.class);
        return valueOf;
    }

    private List convertVOsToInventories(Collection vos) {
        try {
            if (vos.isEmpty()) {
                return new ArrayList();
            }
            Object first = vos.iterator().next();

            Info info = getInfo(first.getClass());
            Method valueOf = getCollectionValueOfMethod(info.inventory, info.inventoryClass);
            List ret = (List) valueOf.invoke(info.inventoryClass, vos);
            return ret;
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    private Object convertVOToInventory(Object vo) {
        try {
            Info info = getInfo(vo.getClass());
            Method valueOf = getValueOfMethod(info.inventoryClass, vo.getClass());
            Object ret = valueOf.invoke(info.inventoryClass, vo);
            return ret;
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }
}
